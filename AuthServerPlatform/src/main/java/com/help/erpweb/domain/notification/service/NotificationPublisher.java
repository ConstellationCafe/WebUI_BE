package com.help.erpweb.domain.notification.service;

/**
 * WebUI_BE 내부 기능이 알림을 발행하는 함수 수준 계약.
 * <p>
 * 호출자의 트랜잭션에 참여한다. 호출자의 작업(예: 포인트 지급)과 알림 저장은 함께
 * 커밋되거나 함께 롤백되고, 실시간 전달은 커밋 이후에만 일어난다(ADR-0004).
 * 대상 회원이 현재 채팅방의 재적 회원이 아니면 {@code NotificationTargetNotFoundException}을 던진다.
 */
public interface NotificationPublisher {
    NotificationPublishResult publish(NotificationCommand command);
}
