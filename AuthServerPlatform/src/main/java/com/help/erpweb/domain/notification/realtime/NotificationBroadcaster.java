package com.help.erpweb.domain.notification.realtime;

/**
 * 저장이 끝난 알림을 접속 중인 회원에게 흘려보내는 경로.
 * 구현은 교체 가능하다(현재 Redis Pub/Sub, 필요하면 Kafka 등). 전달은 at-most-once이며
 * 놓친 알림은 클라이언트가 재접속 시 REST 조회로 복구한다(ADR-0004).
 */
public interface NotificationBroadcaster {
    void broadcast(NotificationMessage message);
}
