package com.help.erpweb.domain.notification.exception;

/** 외부 client가 허용되지 않은 채팅방(botId)으로 발행하려 할 때. */
public class NotificationScopeDeniedException extends RuntimeException {
    public NotificationScopeDeniedException() {
        super("이 API Key로는 해당 채팅방에 알림을 발행할 수 없습니다.");
    }
}
