package com.help.erpweb.domain.notification.dto.request;

import com.help.erpweb.domain.notification.entity.NotificationCategory;
import com.help.erpweb.domain.notification.entity.NotificationTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 외부 시스템(API Key 인증) 알림 발행 요청. 외부 호출에는 로그인한 채팅방 문맥이 없으므로
 * 대상 채팅방(botId)을 명시하고, 서버는 API Key에 허용된 botId인지 확인한다.
 */
public record IntegrationNotificationPublishRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9._:-]{1,64}") String requestId,
        @NotBlank @Size(max = 30) String botId,
        @NotNull NotificationTargetType targetType,
        @Pattern(regexp = "[0-9]{1,20}") String targetDiscordId,
        @NotNull NotificationCategory category,
        @NotBlank @Size(max = 100) String title,
        @NotBlank @Size(max = 1000) String body,
        @Size(max = 255) String link
) {
}
