package com.help.erpweb.domain.notification.controller;

import com.help.erpweb.domain.notification.dto.request.AdminNotificationPublishRequest;
import com.help.erpweb.domain.notification.dto.response.AdminNotificationPageResponse;
import com.help.erpweb.domain.notification.entity.NotificationSource;
import com.help.erpweb.domain.notification.service.NotificationCommand;
import com.help.erpweb.domain.notification.service.NotificationPublishResult;
import com.help.erpweb.domain.notification.service.NotificationService;
import com.help.global.common.response.ApiResponse;
import com.help.global.guild.GuildContext;
import com.help.global.jwt.CustomUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 알림 발행과 발행 이력(ADR-0002 경로 규칙: /api/admin/**).
 * 발행 대상 채팅방은 관리자가 로그인한 채팅방(botId)으로 고정한다.
 */
@RestController
@RequestMapping(AdminNotificationController.BASE_PATH)
@RequiredArgsConstructor
@Validated
@PreAuthorize("@authorization.isAdmin(authentication)")
public class AdminNotificationController {
    static final String BASE_PATH = "/api/admin/notifications";

    private final NotificationService notificationService;

    @PostMapping
    public ApiResponse<NotificationPublishResult> publish(
            @AuthenticationPrincipal CustomUser user,
            @Valid @RequestBody AdminNotificationPublishRequest request
    ) {
        return ApiResponse.success(notificationService.publish(new NotificationCommand(
                GuildContext.requireBotId(),
                request.targetType(),
                request.targetDiscordId(),
                request.category(),
                request.title(),
                request.body(),
                request.link(),
                NotificationSource.ADMIN,
                user.getUsername(),
                request.requestId()
        )));
    }

    @GetMapping
    public ApiResponse<AdminNotificationPageResponse> list(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(notificationService.adminList(GuildContext.requireBotId(), page, size));
    }
}
