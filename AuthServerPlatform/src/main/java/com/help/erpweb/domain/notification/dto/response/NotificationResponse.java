package com.help.erpweb.domain.notification.dto.response;

import com.help.erpweb.domain.notification.entity.Notification;
import com.help.erpweb.domain.notification.entity.NotificationCategory;
import com.help.erpweb.domain.notification.entity.NotificationTargetType;
import java.time.Instant;

/**
 * 회원에게 보이는 알림 한 건. createdAt은 UTC ISO-8601(Z)로 직렬화된다.
 */
public record NotificationResponse(
        long id,
        NotificationCategory category,
        String title,
        String body,
        String link,
        NotificationTargetType targetType,
        Instant createdAt,
        boolean read
) {
    public static NotificationResponse of(Notification notification, long lastReadId) {
        return new NotificationResponse(
                notification.getId(),
                notification.getCategory(),
                notification.getTitle(),
                notification.getBody(),
                notification.getLink(),
                notification.getTargetType(),
                notification.createdInstant(),
                notification.getId() <= lastReadId
        );
    }

    public static NotificationResponse unread(Notification notification) {
        return of(notification, 0L);
    }
}
