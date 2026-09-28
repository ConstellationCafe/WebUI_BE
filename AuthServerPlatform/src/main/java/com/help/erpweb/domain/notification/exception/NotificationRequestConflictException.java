package com.help.erpweb.domain.notification.exception;

public class NotificationRequestConflictException extends RuntimeException {
    public NotificationRequestConflictException() {
        super("같은 요청 ID로 다른 내용의 알림이 이미 발행되었습니다.");
    }
}
