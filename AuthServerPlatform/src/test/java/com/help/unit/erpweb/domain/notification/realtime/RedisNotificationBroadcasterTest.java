package com.help.erpweb.domain.notification.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.help.erpweb.domain.notification.dto.response.NotificationResponse;
import com.help.erpweb.domain.notification.entity.NotificationCategory;
import com.help.erpweb.domain.notification.entity.NotificationTargetType;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.core.StringRedisTemplate;

class RedisNotificationBroadcasterTest {
    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private StringRedisTemplate redisTemplate;
    private NotificationSseRegistry registry;
    private SimpleMeterRegistry meterRegistry;
    private RedisNotificationBroadcaster broadcaster;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        registry = mock(NotificationSseRegistry.class);
        meterRegistry = new SimpleMeterRegistry();
        broadcaster = new RedisNotificationBroadcaster(
                redisTemplate,
                objectMapper,
                registry,
                NotificationRealtimeProperties.defaults(),
                meterRegistry
        );
    }

    private static NotificationMessage message() {
        return new NotificationMessage(
                "bot",
                NotificationTargetType.USER,
                "123",
                new NotificationResponse(
                        7L,
                        NotificationCategory.POINT,
                        "포인트가 입금되었습니다",
                        "500포인트 입금 · 이벤트",
                        "/point_log",
                        NotificationTargetType.USER,
                        Instant.parse("2026-09-28T01:02:03.456Z"),
                        false
                )
        );
    }

    @Test
    void publishesUtcJsonToConfiguredChannel() {
        broadcaster.broadcast(message());

        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate).convertAndSend(eq("webui:notifications"), json.capture());
        assertThat(json.getValue()).contains("\"createdAt\":\"2026-09-28T01:02:03.456Z\"");
        verify(registry, never()).dispatch(any());
    }

    @Test
    void redisFailureFallsBackToLocalConnectionsWithoutRetry() {
        doThrow(new RedisConnectionFailureException("down"))
                .when(redisTemplate).convertAndSend(anyString(), anyString());

        broadcaster.broadcast(message());

        verify(registry).dispatch(message());
        verify(redisTemplate).convertAndSend(anyString(), anyString());
        assertThat(meterRegistry.get("notification.broadcast.failures").tag("stage", "publish").counter().count())
                .isEqualTo(1.0);
    }

    @Test
    void receivedMessageIsDispatchedToLocalConnections() throws Exception {
        byte[] body = objectMapper.writeValueAsString(message()).getBytes(StandardCharsets.UTF_8);

        broadcaster.onMessage(new DefaultMessage("webui:notifications".getBytes(StandardCharsets.UTF_8), body), null);

        verify(registry).dispatch(message());
    }

    @Test
    void malformedMessageIsCountedAndIgnored() {
        broadcaster.onMessage(
                new DefaultMessage(
                        "webui:notifications".getBytes(StandardCharsets.UTF_8),
                        "{not-json".getBytes(StandardCharsets.UTF_8)
                ),
                null
        );

        verify(registry, never()).dispatch(any());
        assertThat(meterRegistry.get("notification.broadcast.failures").tag("stage", "receive").counter().count())
                .isEqualTo(1.0);
    }
}
