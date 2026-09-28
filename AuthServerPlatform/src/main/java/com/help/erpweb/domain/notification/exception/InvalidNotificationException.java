package com.help.erpweb.domain.notification.exception;

/**
 * 발행 요청이 알림 불변식(길이, 대상, 링크 형식 등)을 어겼을 때. 400으로 응답한다.
 * 내부 함수 호출자에게는 잘못된 인자 예외로 동작한다.
 */
public class InvalidNotificationException extends IllegalArgumentException {
    public InvalidNotificationException(String message) {
        super(message);
    }
}
