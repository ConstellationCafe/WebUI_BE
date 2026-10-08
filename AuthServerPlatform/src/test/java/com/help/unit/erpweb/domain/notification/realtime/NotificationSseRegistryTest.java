package com.help.erpweb.domain.notification.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.help.erpweb.domain.notification.dto.response.NotificationResponse;
import com.help.erpweb.domain.notification.dto.response.NotificationUnreadResponse;
import com.help.erpweb.domain.notification.entity.NotificationCategory;
import com.help.erpweb.domain.notification.entity.NotificationTargetType;
import com.help.erpweb.domain.notification.exception.NotificationStreamUnavailableException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter.DataWithMediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class NotificationSseRegistryTest {
    private final List<RecordingEmitter> created = new ArrayList<>();
    private NotificationSseRegistry registry;
    private boolean failSends;

    @BeforeEach
    void setUp() {
        registry = registry(new NotificationRealtimeProperties("test", 60_000L, 25_000L, 2, 4));
        registry.start();
    }

    private NotificationSseRegistry registry(NotificationRealtimeProperties properties) {
        return new NotificationSseRegistry(properties, new SimpleMeterRegistry(), timeout -> {
            RecordingEmitter emitter = new RecordingEmitter(timeout);
            created.add(emitter);
            return emitter;
        });
    }

    private static NotificationUnreadResponse ready() {
        return new NotificationUnreadResponse(1L, 5L, 4L);
    }

    private static NotificationMessage message(String botId, NotificationTargetType type, String target, long id) {
        return new NotificationMessage(
                botId,
                type,
                target,
                new NotificationResponse(
                        id,
                        NotificationCategory.ANNOUNCEMENT,
                        "제목",
                        "본문",
                        null,
                        type,
                        Instant.parse("2026-09-28T00:00:00Z"),
                        false
                )
        );
    }

    @Test
    void firstEventIsReadySummary() {
        RecordingEmitter emitter = (RecordingEmitter) registry.register("bot", "1", ready());

        assertThat(emitter.events()).hasSize(1);
        assertThat(emitter.events().get(0)).contains("event:" + NotificationSseRegistry.EVENT_READY);
        assertThat(registry.connectionCount()).isEqualTo(1);
    }

    @Test
    void guildMessageReachesOnlyMembersOfThatGuild() {
        RecordingEmitter sameGuildA = (RecordingEmitter) registry.register("bot", "1", ready());
        RecordingEmitter sameGuildB = (RecordingEmitter) registry.register("bot", "2", ready());
        RecordingEmitter otherGuild = (RecordingEmitter) registry.register("other", "1", ready());

        registry.dispatch(message("bot", NotificationTargetType.GUILD, null, 10L));

        assertThat(sameGuildA.events()).hasSize(2);
        assertThat(sameGuildA.events().get(1)).contains("event:notification").contains("id:10");
        assertThat(sameGuildB.events()).hasSize(2);
        assertThat(otherGuild.events()).hasSize(1);
    }

    @Test
    void userMessageReachesOnlyTargetMember() {
        RecordingEmitter target = (RecordingEmitter) registry.register("bot", "1", ready());
        RecordingEmitter other = (RecordingEmitter) registry.register("bot", "2", ready());

        registry.dispatch(message("bot", NotificationTargetType.USER, "1", 11L));

        assertThat(target.events()).hasSize(2);
        assertThat(other.events()).hasSize(1);
    }

    @Test
    void perUserLimitClosesOldestConnection() {
        RecordingEmitter first = (RecordingEmitter) registry.register("bot", "1", ready());
        registry.register("bot", "1", ready());
        registry.register("bot", "1", ready());

        assertThat(first.completed).isTrue();
        assertThat(registry.connectionCount()).isEqualTo(2);
    }

    @Test
    void instanceLimitRejectsNewConnections() {
        registry.register("bot", "1", ready());
        registry.register("bot", "2", ready());
        registry.register("bot", "3", ready());
        registry.register("bot", "4", ready());

        assertThatThrownBy(() -> registry.register("bot", "5", ready()))
                .isInstanceOf(NotificationStreamUnavailableException.class);
        assertThat(registry.connectionCount()).isEqualTo(4);
    }

    @Test
    void brokenConnectionIsRemovedOnSendFailure() {
        registry.register("bot", "1", ready());
        failSends = true;

        registry.dispatch(message("bot", NotificationTargetType.GUILD, null, 12L));

        assertThat(registry.connectionCount()).isZero();
    }

    @Test
    void stoppedRegistryClosesConnectionsAndRejectsNewOnes() {
        RecordingEmitter emitter = (RecordingEmitter) registry.register("bot", "1", ready());

        registry.stop();

        assertThat(emitter.completed).isTrue();
        assertThat(registry.connectionCount()).isZero();
        assertThatThrownBy(() -> registry.register("bot", "1", ready()))
                .isInstanceOf(NotificationStreamUnavailableException.class);
    }

    @Test
    void heartbeatKeepsHealthyConnections() {
        RecordingEmitter emitter = (RecordingEmitter) registry.register("bot", "1", ready());

        registry.heartbeat();

        assertThat(emitter.events()).hasSize(2);
        assertThat(emitter.events().get(1)).contains(":ping");
        assertThat(registry.connectionCount()).isEqualTo(1);
    }

    /** 실제 HTTP 응답 없이 보낸 SSE 이벤트를 문자열로 모아 검증한다. */
    private final class RecordingEmitter extends SseEmitter {
        private final List<String> sent = new ArrayList<>();
        private boolean completed;

        RecordingEmitter(Long timeout) {
            super(timeout);
        }

        @Override
        public void send(SseEventBuilder builder) throws IOException {
            if (failSends) {
                throw new IOException("client gone");
            }
            Set<DataWithMediaType> parts = builder.build();
            StringBuilder text = new StringBuilder();
            for (DataWithMediaType part : parts) {
                text.append(part.getData());
            }
            sent.add(text.toString());
        }

        @Override
        public void complete() {
            completed = true;
        }

        List<String> events() {
            return sent;
        }
    }
}
