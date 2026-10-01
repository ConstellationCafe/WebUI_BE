package com.help.erpweb.domain.notification.realtime;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 실시간 알림 설정. 값을 바꾸면 README의 외부 연동·운영 표를 함께 갱신한다.
 *
 * @param channel               인스턴스 간 전달에 쓰는 Redis Pub/Sub channel
 * @param emitterTimeoutMillis  SSE 연결 최대 유지 시간. 만료되면 클라이언트가 다시 연결한다
 * @param heartbeatMillis       중간 proxy의 idle timeout을 피하기 위한 heartbeat 주기
 * @param maxConnectionsPerUser 한 회원(채팅방 단위)의 동시 연결 수. 초과하면 가장 오래된 연결을 닫는다
 * @param maxConnections        인스턴스 전체 동시 연결 수(bulkhead). 초과하면 503
 */
@ConfigurationProperties(prefix = "notification.realtime")
public record NotificationRealtimeProperties(
        String channel,
        Long emitterTimeoutMillis,
        Long heartbeatMillis,
        Integer maxConnectionsPerUser,
        Integer maxConnections
) {
    public NotificationRealtimeProperties {
        channel = channel == null || channel.isBlank() ? "webui:notifications" : channel;
        emitterTimeoutMillis = emitterTimeoutMillis == null ? 30 * 60 * 1000L : emitterTimeoutMillis;
        heartbeatMillis = heartbeatMillis == null ? 25_000L : heartbeatMillis;
        maxConnectionsPerUser = maxConnectionsPerUser == null ? 5 : maxConnectionsPerUser;
        maxConnections = maxConnections == null ? 1000 : maxConnections;
    }

    public static NotificationRealtimeProperties defaults() {
        return new NotificationRealtimeProperties(null, null, null, null, null);
    }
}
