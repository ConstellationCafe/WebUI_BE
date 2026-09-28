package com.help.erpweb.domain.notification.realtime;

import com.help.erpweb.domain.notification.exception.NotificationStreamUnavailableException;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongFunction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 이 인스턴스에 연결된 SSE 구독자 목록.
 * <p>
 * 구독자는 (botId, discordId) 단위로 묶고, 목록 변경은 {@link ConcurrentHashMap#compute}
 * 안에서만 해서 같은 회원의 등록/해제가 경합해도 연결 수 집계가 어긋나지 않게 한다.
 * 전달 중 끊긴 연결은 즉시 제거한다. 종료 시에는 graceful shutdown보다 먼저 모든 연결을
 * 닫아 진행 중 요청 대기가 SSE 때문에 제한 시간까지 늘어나지 않게 한다.
 */
@Slf4j
@Component
public class NotificationSseRegistry implements SmartLifecycle {
    static final String EVENT_READY = "ready";
    static final String EVENT_NOTIFICATION = "notification";

    private final NotificationRealtimeProperties properties;
    private final LongFunction<SseEmitter> emitterFactory;
    private final Map<String, List<Subscriber>> subscribers = new ConcurrentHashMap<>();
    private final AtomicInteger connections = new AtomicInteger();
    private volatile boolean running;

    @Autowired
    public NotificationSseRegistry(NotificationRealtimeProperties properties, MeterRegistry meterRegistry) {
        this(properties, meterRegistry, SseEmitter::new);
    }

    NotificationSseRegistry(
            NotificationRealtimeProperties properties,
            MeterRegistry meterRegistry,
            LongFunction<SseEmitter> emitterFactory
    ) {
        this.properties = properties;
        this.emitterFactory = emitterFactory;
        Gauge.builder("notification.sse.connections", connections, AtomicInteger::get)
                .description("이 인스턴스의 실시간 알림 연결 수")
                .register(meterRegistry);
    }

    /**
     * 새 구독을 등록하고 첫 이벤트로 {@code ready}(읽지 않은 알림 요약)를 보낸다.
     * 클라이언트는 연결될 때마다 이 값으로 빨간 점 상태를 다시 맞춘다.
     */
    public SseEmitter register(String botId, String discordId, Object readyPayload) {
        if (!running) {
            throw new NotificationStreamUnavailableException();
        }
        if (connections.incrementAndGet() > properties.maxConnections()) {
            connections.decrementAndGet();
            throw new NotificationStreamUnavailableException();
        }
        SseEmitter emitter = emitterFactory.apply(properties.emitterTimeoutMillis());
        Subscriber subscriber = new Subscriber(botId, discordId, emitter);
        List<Subscriber> evicted = new CopyOnWriteArrayList<>();
        subscribers.compute(key(botId, discordId), (key, current) -> {
            List<Subscriber> list = current == null ? new CopyOnWriteArrayList<>() : current;
            list.add(subscriber);
            while (list.size() > properties.maxConnectionsPerUser()) {
                Subscriber oldest = list.remove(0);
                connections.decrementAndGet();
                evicted.add(oldest);
            }
            return list;
        });
        evicted.forEach(old -> completeQuietly(old.emitter()));

        emitter.onCompletion(() -> remove(subscriber));
        emitter.onTimeout(() -> {
            remove(subscriber);
            completeQuietly(emitter);
        });
        emitter.onError(error -> remove(subscriber));

        send(subscriber, SseEmitter.event().name(EVENT_READY).data(readyPayload, MediaType.APPLICATION_JSON));
        return emitter;
    }

    /** 메시지를 받을 자격이 있는 이 인스턴스의 모든 구독자에게 보낸다. */
    public void dispatch(NotificationMessage message) {
        for (List<Subscriber> list : subscribers.values()) {
            for (Subscriber subscriber : list) {
                if (message.isFor(subscriber.botId(), subscriber.discordId())) {
                    send(subscriber, SseEmitter.event()
                            .id(String.valueOf(message.notification().id()))
                            .name(EVENT_NOTIFICATION)
                            .data(message.notification(), MediaType.APPLICATION_JSON));
                }
            }
        }
    }

    @Scheduled(
            fixedDelayString = "${notification.realtime.heartbeat-millis:25000}",
            initialDelayString = "${notification.realtime.heartbeat-millis:25000}"
    )
    public void heartbeat() {
        for (List<Subscriber> list : subscribers.values()) {
            for (Subscriber subscriber : list) {
                send(subscriber, SseEmitter.event().comment("ping"));
            }
        }
    }

    public int connectionCount() {
        return connections.get();
    }

    private void send(Subscriber subscriber, SseEmitter.SseEventBuilder event) {
        try {
            subscriber.emitter().send(event);
        } catch (IOException | IllegalStateException ex) {
            // 클라이언트가 이미 떠난 연결. 정상 흐름이므로 원인만 debug로 남긴다.
            log.debug("실시간 알림 전송 실패로 연결을 정리합니다: {}", ex.getClass().getSimpleName());
            remove(subscriber);
            completeQuietly(subscriber.emitter());
        }
    }

    private void remove(Subscriber subscriber) {
        subscribers.computeIfPresent(key(subscriber.botId(), subscriber.discordId()), (key, list) -> {
            if (list.remove(subscriber)) {
                connections.decrementAndGet();
            }
            return list.isEmpty() ? null : list;
        });
    }

    private static void completeQuietly(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (RuntimeException ignored) {
            // 이미 완료된 emitter
        }
    }

    private static String key(String botId, String discordId) {
        return botId + ':' + discordId;
    }

    @Override
    public void start() {
        running = true;
    }

    @Override
    public void stop() {
        running = false;
        for (List<Subscriber> list : subscribers.values()) {
            for (Subscriber subscriber : list) {
                completeQuietly(subscriber.emitter());
            }
        }
        subscribers.clear();
        connections.set(0);
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    /**
     * 기본 phase(가장 늦게 시작, 가장 먼저 종료)를 써서 웹 서버 graceful shutdown 단계보다
     * 먼저 SSE 연결을 닫는다.
     */
    @Override
    public int getPhase() {
        return SmartLifecycle.DEFAULT_PHASE;
    }

    private record Subscriber(String botId, String discordId, SseEmitter emitter) {
        // emitter마다 다른 구독자이므로 동일성은 emitter 기준으로 비교한다.
        @Override
        public boolean equals(Object other) {
            return other instanceof Subscriber that && that.emitter == emitter;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(emitter);
        }
    }
}
