package com.help.erpweb.domain.notification.exception;

public class NotificationTargetNotFoundException extends RuntimeException {
    public NotificationTargetNotFoundException() {
        super("현재 채팅방의 재적 회원을 찾을 수 없습니다.");
    }
}
