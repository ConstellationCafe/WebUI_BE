package com.help.erpweb.domain.notification.controller;

import com.help.erpweb.domain.notification.dto.request.NotificationReadCursorRequest;
import com.help.erpweb.domain.notification.dto.response.NotificationSliceResponse;
import com.help.erpweb.domain.notification.dto.response.NotificationUnreadResponse;
import com.help.erpweb.domain.notification.realtime.NotificationSseRegistry;
import com.help.erpweb.domain.notification.service.NotificationService;
import com.help.global.common.response.ApiResponse;
import com.help.global.guild.GuildContext;
import com.help.global.jwt.CustomUser;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 로그인한 회원 본인의 알림. 채팅방은 토큰의 botId(GuildContext), 회원은 principal로만
 * 결정하므로 다른 회원·다른 채팅방의 알림은 조회할 수 없다.
 */
@RestController
@RequestMapping(MyNotificationController.BASE_PATH)
@RequiredArgsConstructor
@Validated
public class MyNotificationController {
    static final String BASE_PATH = "/api/me/notifications";

    private final NotificationService notificationService;
    private final NotificationSseRegistry sseRegistry;

    @GetMapping
    public ApiResponse<NotificationSliceResponse> list(
            @AuthenticationPrincipal CustomUser user,
            @RequestParam(required = false) @Min(1) Long beforeId,
            @RequestParam(defaultValue = "20") @Min(1) @Max(NotificationService.LIST_MAX_SIZE) int size
    ) {
        return ApiResponse.success(
                notificationService.list(GuildContext.requireBotId(), user.getUsername(), beforeId, size)
        );
    }

    @GetMapping("/unread-count")
    public ApiResponse<NotificationUnreadResponse> unreadCount(@AuthenticationPrincipal CustomUser user) {
        return ApiResponse.success(notificationService.unread(GuildContext.requireBotId(), user.getUsername()));
    }

    /** 멱등. 같은 값을 여러 번 보내거나 더 작은 값을 보내도 읽음 위치는 뒤로 가지 않는다. */
    @PutMapping("/read-cursor")
    public ApiResponse<NotificationUnreadResponse> markRead(
            @AuthenticationPrincipal CustomUser user,
            @Valid @RequestBody NotificationReadCursorRequest request
    ) {
        return ApiResponse.success(notificationService.markRead(
                GuildContext.requireBotId(),
                user.getUsername(),
                request.lastReadId()
        ));
    }

    /**
     * 실시간 알림 스트림(text/event-stream). 첫 이벤트 {@code ready}는 읽지 않은 알림 요약,
     * 이후 {@code notification} 이벤트가 새 알림 한 건씩이다. 연결이 끊기면 클라이언트는
     * 다시 연결하고 ready 값으로 상태를 맞춘다.
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@AuthenticationPrincipal CustomUser user, HttpServletResponse response) {
        String botId = GuildContext.requireBotId();
        String discordId = user.getUsername();
        response.setHeader("Cache-Control", "no-cache");
        // nginx 등 reverse proxy가 응답을 모았다가 보내지 않도록 한다.
        response.setHeader("X-Accel-Buffering", "no");
        return sseRegistry.register(botId, discordId, notificationService.unread(botId, discordId));
    }
}
