package com.help.erpweb.domain.notification.exception;

/** 실시간 연결 수 제한(인스턴스 전체) 초과 또는 종료 중이라 새 연결을 받을 수 없을 때. */
public class NotificationStreamUnavailableException extends RuntimeException {
    public NotificationStreamUnavailableException() {
        super("실시간 알림 연결을 잠시 사용할 수 없습니다. 잠시 후 다시 시도해 주세요.");
    }
}
