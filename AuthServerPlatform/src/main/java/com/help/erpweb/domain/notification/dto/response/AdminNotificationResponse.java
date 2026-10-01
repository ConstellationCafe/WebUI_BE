package com.help.erpweb.domain.notification.dto.response;

import com.help.erpweb.domain.notification.entity.Notification;
import com.help.erpweb.domain.notification.entity.NotificationCategory;
import com.help.erpweb.domain.notification.entity.NotificationSource;
import com.help.erpweb.domain.notification.entity.NotificationTargetType;
import java.time.Instant;

/** 발행 결과와 관리자 발행 이력에 쓰는 알림 표현. 발행 경로와 발행자를 포함한다. */
public record AdminNotificationResponse(
        long id,
        NotificationTargetType targetType,
        String targetDiscordId,
        NotificationCategory category,
        String title,
        String body,
        String link,
        NotificationSource source,
        String sourceRef,
        Instant createdAt
) {
    public static AdminNotificationResponse of(Notification notification) {
        return new AdminNotificationResponse(
                notification.getId(),
                notification.getTargetType(),
                notification.getTargetDiscordId(),
                notification.getCategory(),
                notification.getTitle(),
                notification.getBody(),
                notification.getLink(),
                notification.getSource(),
                notification.getSourceRef(),
                notification.createdInstant()
        );
    }
}
