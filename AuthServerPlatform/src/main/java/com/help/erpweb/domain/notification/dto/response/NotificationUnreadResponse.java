package com.help.erpweb.domain.notification.dto.response;

/**
 * @param latestId 회원이 볼 수 있는 가장 최신 알림 ID. 알림이 없으면 null
 */
public record NotificationUnreadResponse(
        long unreadCount,
        Long latestId,
        long lastReadId
) {
}
