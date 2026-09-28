package com.help.erpweb.domain.notification.controller;

import com.help.erpweb.domain.notification.dto.request.IntegrationNotificationPublishRequest;
import com.help.erpweb.domain.notification.entity.NotificationSource;
import com.help.erpweb.domain.notification.exception.NotificationScopeDeniedException;
import com.help.erpweb.domain.notification.service.NotificationCommand;
import com.help.erpweb.domain.notification.service.NotificationPublishResult;
import com.help.erpweb.domain.notification.service.NotificationService;
import com.help.global.common.response.ApiResponse;
import com.help.global.integration.IntegrationClient;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 외부 시스템용 알림 발행. {@code X-Api-Key}로 인증하며(IntegrationSecurityConfig),
 * client에 허용된 채팅방(botId)에만 발행할 수 있다.
 * 네트워크 오류로 재시도할 때는 같은 requestId를 보내야 중복 발행되지 않는다.
 */
@RestController
@RequestMapping(IntegrationNotificationController.BASE_PATH)
@RequiredArgsConstructor
public class IntegrationNotificationController {
    static final String BASE_PATH = "/api/integrations/notifications";

    private final NotificationService notificationService;

    @PostMapping
    public ApiResponse<NotificationPublishResult> publish(
            @AuthenticationPrincipal IntegrationClient client,
            @Valid @RequestBody IntegrationNotificationPublishRequest request
    ) {
        if (client == null || !client.canPublishTo(request.botId())) {
            throw new NotificationScopeDeniedException();
        }
        return ApiResponse.success(notificationService.publish(new NotificationCommand(
                request.botId(),
                request.targetType(),
                request.targetDiscordId(),
                request.category(),
                request.title(),
                request.body(),
                request.link(),
                NotificationSource.EXTERNAL,
                client.id(),
                request.requestId()
        )));
    }
}
