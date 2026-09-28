package com.help.erpweb.domain.notification.realtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis Pub/Sub 기반 전달. 모든 인스턴스가 같은 channel을 구독하므로, 어느 인스턴스에서
 * 발행해도 회원이 연결된 인스턴스가 SSE로 내보낸다.
 * <p>
 * Pub/Sub은 메시지를 보관하지 않는다. Redis 발행이 실패하면 자동 재시도하지 않고
 * (중복 전달·요청 지연 위험) 이 인스턴스의 구독자에게만 직접 전달한 뒤 실패를 metric으로
 * 남긴다. 다른 인스턴스의 회원은 재접속·목록 조회 때 DB에서 복구한다(ADR-0004).
 */
@Slf4j
@Component
public class RedisNotificationBroadcaster implements NotificationBroadcaster, MessageListener {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final NotificationSseRegistry registry;
    private final NotificationRealtimeProperties properties;
    private final Counter publishFailures;
    private final Counter receiveFailures;

    public RedisNotificationBroadcaster(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            NotificationSseRegistry registry,
            NotificationRealtimeProperties properties,
            MeterRegistry meterRegistry
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.registry = registry;
        this.properties = properties;
        this.publishFailures = Counter.builder("notification.broadcast.failures")
                .tag("stage", "publish")
                .register(meterRegistry);
        this.receiveFailures = Counter.builder("notification.broadcast.failures")
                .tag("stage", "receive")
                .register(meterRegistry);
    }

    @Override
    public void broadcast(NotificationMessage message) {
        try {
            redisTemplate.convertAndSend(properties.channel(), objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException | RuntimeException ex) {
            publishFailures.increment();
            log.warn(
                    "알림 실시간 발행 실패, 로컬 연결에만 전달합니다 - notificationId={}, reason={}",
                    message.notification().id(),
                    ex.getClass().getSimpleName()
            );
            registry.dispatch(message);
        }
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody(), StandardCharsets.UTF_8);
            registry.dispatch(objectMapper.readValue(json, NotificationMessage.class));
        } catch (JsonProcessingException | RuntimeException ex) {
            receiveFailures.increment();
            log.warn("알림 실시간 메시지를 처리하지 못했습니다 - reason={}", ex.getClass().getSimpleName());
        }
    }
}
