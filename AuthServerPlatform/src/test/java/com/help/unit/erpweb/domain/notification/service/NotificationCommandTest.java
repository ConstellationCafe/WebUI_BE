package com.help.erpweb.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.help.erpweb.domain.notification.entity.NotificationCategory;
import com.help.erpweb.domain.notification.entity.NotificationSource;
import com.help.erpweb.domain.notification.entity.NotificationTargetType;
import com.help.erpweb.domain.notification.exception.InvalidNotificationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class NotificationCommandTest {

    private static NotificationCommand command(
            NotificationTargetType targetType,
            String targetDiscordId,
            String title,
            String link,
            String requestKey
    ) {
        return new NotificationCommand(
                "1001",
                targetType,
                targetDiscordId,
                NotificationCategory.ANNOUNCEMENT,
                title,
                "본문",
                link,
                NotificationSource.ADMIN,
                "42",
                requestKey
        );
    }

    @Test
    void trimsTextAndTreatsBlankOptionalValuesAsAbsent() {
        NotificationCommand command = command(NotificationTargetType.GUILD, "  ", "  공지  ", " ", " ");

        assertThat(command.title()).isEqualTo("공지");
        assertThat(command.targetDiscordId()).isNull();
        assertThat(command.link()).isNull();
        assertThat(command.requestKey()).isNull();
    }

    @Test
    void botIdFollowsDatabaseColumnNotNumericFormat() {
        assertThat(commandForBot(" constellation_bot ").botId()).isEqualTo("constellation_bot");
        assertThat(commandForBot("a".repeat(30)).botId()).hasSize(30);

        assertThatThrownBy(() -> commandForBot("a".repeat(31)))
                .isInstanceOf(InvalidNotificationException.class);
        assertThatThrownBy(() -> commandForBot("  "))
                .isInstanceOf(InvalidNotificationException.class);
    }

    private static NotificationCommand commandForBot(String botId) {
        return new NotificationCommand(
                botId,
                NotificationTargetType.GUILD,
                null,
                NotificationCategory.EVENT,
                "제목",
                "본문",
                null,
                NotificationSource.EXTERNAL,
                "discord-bot",
                "competition:1"
        );
    }

    @Test
    void userNotificationRequiresNumericTargetDiscordId() {
        assertThatThrownBy(() -> command(NotificationTargetType.USER, null, "제목", null, null))
                .isInstanceOf(InvalidNotificationException.class);
        assertThatThrownBy(() -> command(NotificationTargetType.USER, "abc", "제목", null, null))
                .isInstanceOf(InvalidNotificationException.class);

        assertThat(command(NotificationTargetType.USER, "123", "제목", null, null).targetDiscordId())
                .isEqualTo("123");
    }

    @Test
    void guildNotificationRejectsTargetDiscordIdToAvoidAccidentalBroadcast() {
        assertThatThrownBy(() -> command(NotificationTargetType.GUILD, "123", "제목", null, null))
                .isInstanceOf(InvalidNotificationException.class);
    }

    @Test
    void titleMustBePresentAndWithinLimit() {
        assertThatThrownBy(() -> command(NotificationTargetType.GUILD, null, "   ", null, null))
                .isInstanceOf(InvalidNotificationException.class);
        assertThatThrownBy(() -> command(NotificationTargetType.GUILD, null, "가".repeat(101), null, null))
                .isInstanceOf(InvalidNotificationException.class);

        assertThat(command(NotificationTargetType.GUILD, null, "가".repeat(100), null, null).title())
                .hasSize(100);
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://evil.example", "//evil.example", "javascript:alert(1)", "point_log", "/a b"})
    void linkOnlyAllowsInternalPaths(String link) {
        assertThatThrownBy(() -> command(NotificationTargetType.GUILD, null, "제목", link, null))
                .isInstanceOf(InvalidNotificationException.class);
    }

    @Test
    void acceptsInternalLinkWithQuery() {
        assertThat(command(NotificationTargetType.GUILD, null, "제목", "/home?guild_id=1", null).link())
                .isEqualTo("/home?guild_id=1");
    }

    @Test
    void requestKeyAllowsUuidAndRejectsUnsafeCharacters() {
        assertThat(command(
                NotificationTargetType.GUILD,
                null,
                "제목",
                null,
                "0b8f6a0e-3c1d-4f7a-9d3e-1f2a3b4c5d6e"
        ).requestKey()).isEqualTo("0b8f6a0e-3c1d-4f7a-9d3e-1f2a3b4c5d6e");

        assertThatThrownBy(() -> command(NotificationTargetType.GUILD, null, "제목", null, "a/b"))
                .isInstanceOf(InvalidNotificationException.class);
    }

    @Test
    void internalFactoryUsesInternalSourceWithoutRequestKey() {
        NotificationCommand command = NotificationCommand.internal(
                "1001",
                NotificationTargetType.USER,
                "123",
                NotificationCategory.POINT,
                "포인트가 입금되었습니다",
                "500포인트 입금",
                "/point_log",
                "point"
        );

        assertThat(command.source()).isEqualTo(NotificationSource.INTERNAL);
        assertThat(command.sourceRef()).isEqualTo("point");
        assertThat(command.requestKey()).isNull();
    }
}
