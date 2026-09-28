package com.help.erpweb.domain.notification.dto.request;

import com.help.erpweb.domain.notification.entity.NotificationCategory;
import com.help.erpweb.domain.notification.entity.NotificationTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 관리자 알림 발행 요청. {@code requestId}는 화면에서 발행 시도마다 만드는 값(UUID 권장)으로,
 * 네트워크 오류로 다시 보내도 알림이 두 번 생기지 않게 한다.
 * 대상 조합(USER면 targetDiscordId 필수 등)은 도메인 명령에서 한 번 더 검증한다.
 */
public record AdminNotificationPublishRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9._:-]{1,64}") String requestId,
        @NotNull NotificationTargetType targetType,
        @Pattern(regexp = "[0-9]{1,20}") String targetDiscordId,
        @NotNull NotificationCategory category,
        @NotBlank @Size(max = 100) String title,
        @NotBlank @Size(max = 1000) String body,
        @Size(max = 255) String link
) {
}
