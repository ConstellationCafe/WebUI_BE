package com.help.erpweb.domain.notification.entity;

/**
 * 알림 수신 범위. GUILD는 채팅방(botId)의 모든 회원, USER는 한 회원에게만 보인다.
 */
public enum NotificationTargetType {
    GUILD,
    USER
}
