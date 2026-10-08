package com.help.erpweb.domain.notification.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.help.erpweb.domain.notification.dto.request.AdminNotificationPublishRequest;
import com.help.erpweb.domain.notification.dto.request.IntegrationNotificationPublishRequest;
import com.help.erpweb.domain.notification.dto.response.NotificationUnreadResponse;
import com.help.erpweb.domain.notification.entity.NotificationCategory;
import com.help.erpweb.domain.notification.entity.NotificationSource;
import com.help.erpweb.domain.notification.entity.NotificationTargetType;
import com.help.erpweb.domain.notification.exception.NotificationScopeDeniedException;
import com.help.erpweb.domain.notification.realtime.NotificationSseRegistry;
import com.help.erpweb.domain.notification.service.NotificationCommand;
import com.help.erpweb.domain.notification.service.NotificationService;
import com.help.global.data.Authority;
import com.help.global.guild.GuildContext;
import com.help.global.integration.IntegrationClient;
import com.help.global.jwt.CustomUser;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class NotificationControllersTest {
    private final NotificationService service = mock(NotificationService.class);
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @AfterEach
    void clearGuild() {
        GuildContext.clear();
    }

    private static CustomUser user(String discordId, String role) {
        return CustomUser.of(discordId, "OAUTH_USER", role, null);
    }

    @Test
    void controllersAreMountedUnderTheirSecurityPrefixes() {
        assertThat(AdminNotificationController.class.getAnnotation(RequestMapping.class).value())
                .containsExactly("/api/admin/notifications");
        assertThat(AdminNotificationController.class.getAnnotation(PreAuthorize.class)).isNotNull();
        assertThat(MyNotificationController.class.getAnnotation(RequestMapping.class).value())
                .containsExactly("/api/me/notifications");
        assertThat(IntegrationNotificationController.class.getAnnotation(RequestMapping.class).value())
                .containsExactly("/api/integrations/notifications");
    }

    @Test
    void adminPublishIsScopedToLoggedInGuildAndRecordsIssuer() {
        GuildContext.setBotId("1001");
        String requestId = UUID.randomUUID().toString();

        new AdminNotificationController(service).publish(
                user("42", Authority.ADMIN),
                new AdminNotificationPublishRequest(
                        requestId,
                        NotificationTargetType.GUILD,
                        null,
                        NotificationCategory.ANNOUNCEMENT,
                        "점검 안내",
                        "오늘 밤 점검합니다.",
                        null
                )
        );

        ArgumentCaptor<NotificationCommand> command = ArgumentCaptor.forClass(NotificationCommand.class);
        verify(service).publish(command.capture());
        assertThat(command.getValue().botId()).isEqualTo("1001");
        assertThat(command.getValue().source()).isEqualTo(NotificationSource.ADMIN);
        assertThat(command.getValue().sourceRef()).isEqualTo("42");
        assertThat(command.getValue().requestKey()).isEqualTo(requestId);
    }

    @Test
    void integrationClientCannotPublishToGuildOutsideItsScope() {
        IntegrationNotificationController controller = new IntegrationNotificationController(service);
        IntegrationClient client = new IntegrationClient("discord-bot", Set.of("1001"));

        assertThatThrownBy(() -> controller.publish(client, integrationRequest("2002")))
                .isInstanceOf(NotificationScopeDeniedException.class);
        verifyNoInteractions(service);
    }

    @Test
    void integrationPublishUsesClientIdAsIssuer() {
        IntegrationClient client = new IntegrationClient("discord-bot", Set.of("1001"));

        new IntegrationNotificationController(service).publish(client, integrationRequest("1001"));

        ArgumentCaptor<NotificationCommand> command = ArgumentCaptor.forClass(NotificationCommand.class);
        verify(service).publish(command.capture());
        assertThat(command.getValue().source()).isEqualTo(NotificationSource.EXTERNAL);
        assertThat(command.getValue().sourceRef()).isEqualTo("discord-bot");
        assertThat(command.getValue().botId()).isEqualTo("1001");
    }

    @Test
    void streamDisablesProxyBufferingAndRegistersCurrentMember() {
        GuildContext.setBotId("1001");
        NotificationSseRegistry registry = mock(NotificationSseRegistry.class);
        NotificationUnreadResponse unread = new NotificationUnreadResponse(2L, 9L, 7L);
        SseEmitter emitter = new SseEmitter();
        when(service.unread("1001", "42")).thenReturn(unread);
        when(registry.register(eq("1001"), eq("42"), any())).thenReturn(emitter);
        MockHttpServletResponse response = new MockHttpServletResponse();

        SseEmitter result = new MyNotificationController(service, registry)
                .stream(user("42", Authority.USER), response);

        assertThat(result).isSameAs(emitter);
        assertThat(response.getHeader("X-Accel-Buffering")).isEqualTo("no");
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-cache");
        verify(registry).register("1001", "42", unread);
    }

    @Test
    void adminRequestValidationRejectsOversizedAndMissingFields() {
        AdminNotificationPublishRequest invalid = new AdminNotificationPublishRequest(
                "bad id with spaces",
                null,
                "not-number",
                NotificationCategory.ANNOUNCEMENT,
                "가".repeat(101),
                " ",
                "/" + "a".repeat(255)
        );

        assertThat(validator.validate(invalid))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("requestId", "targetType", "targetDiscordId", "title", "body", "link");
    }

    private static IntegrationNotificationPublishRequest integrationRequest(String botId) {
        return new IntegrationNotificationPublishRequest(
                "bot-event-1",
                botId,
                NotificationTargetType.USER,
                "123",
                NotificationCategory.EVENT,
                "대회 시작",
                "지금 대회가 시작되었습니다.",
                "/home"
        );
    }
}
