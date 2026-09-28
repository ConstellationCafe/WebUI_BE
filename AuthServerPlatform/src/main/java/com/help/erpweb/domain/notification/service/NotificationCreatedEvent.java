package com.help.erpweb.domain.notification.service;

import com.help.erpweb.domain.notification.realtime.NotificationMessage;

/** 알림이 저장됐다는 도메인 이벤트. 커밋 이후 실시간 전달의 입력이 된다. */
public record NotificationCreatedEvent(NotificationMessage message) {
}
