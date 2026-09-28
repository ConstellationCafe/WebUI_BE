package com.help.erpweb.domain.notification.realtime;

import com.help.erpweb.domain.notification.dto.response.NotificationResponse;
import com.help.erpweb.domain.notification.entity.Notification;
import com.help.erpweb.domain.notification.entity.NotificationTargetType;
import java.util.Objects;

/**
 * 인스턴스 사이(Redis channel)로 오가는 실시간 전달 메시지.
 * 라우팅 정보(botId, 대상)와 회원에게 보낼 본문을 함께 담는다.
 */
public record NotificationMessage(
        String botId,
        NotificationTargetType targetType,
        String targetDiscordId,
        NotificationResponse notification
) {
    public static NotificationMessage from(Notification notification) {
        return new NotificationMessage(
                notification.getBotId(),
                notification.getTargetType(),
                notification.getTargetDiscordId(),
                NotificationResponse.unread(notification)
        );
    }

    public boolean isFor(String subscriberBotId, String subscriberDiscordId) {
        if (!Objects.equals(botId, subscriberBotId)) {
            return false;
        }
        return targetType == NotificationTargetType.GUILD
                || Objects.equals(targetDiscordId, subscriberDiscordId);
    }
}
