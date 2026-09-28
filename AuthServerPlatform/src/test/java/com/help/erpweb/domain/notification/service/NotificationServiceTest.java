package com.help.erpweb.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.help.erpweb.domain.notification.dto.response.NotificationResponse;
import com.help.erpweb.domain.notification.dto.response.NotificationSliceResponse;
import com.help.erpweb.domain.notification.dto.response.NotificationUnreadResponse;
import com.help.erpweb.domain.notification.entity.Notification;
import com.help.erpweb.domain.notification.entity.NotificationCategory;
import com.help.erpweb.domain.notification.entity.NotificationSource;
import com.help.erpweb.domain.notification.entity.NotificationTargetType;
import com.help.erpweb.domain.notification.exception.NotificationRequestConflictException;
import com.help.erpweb.domain.notification.exception.NotificationTargetNotFoundException;
import com.help.erpweb.domain.notification.repository.NotificationReadCursorRepository;
import com.help.erpweb.domain.notification.repository.NotificationRepository;
import com.help.global.discord.identity.DiscordUser;
import com.help.global.discord.identity.DiscordUserRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    private static final String BOT = "1001";
    private static final Instant NOW = Instant.parse("2026-09-28T01:02:03.456789Z");
    private static final Instant NOW_MILLIS = Instant.parse("2026-09-28T01:02:03.456Z");

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationReadCursorRepository readCursorRepository;
    @Mock
    private DiscordUserRepository discordUserRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(
                notificationRepository,
                readCursorRepository,
                discordUserRepository,
                eventPublisher,
                Clock.fixed(NOW, ZoneOffset.UTC),
                new SimpleMeterRegistry()
        );
    }

    private static NotificationCommand guildCommand(String title, String requestKey) {
        return new NotificationCommand(
                BOT,
                NotificationTargetType.GUILD,
                null,
                NotificationCategory.ANNOUNCEMENT,
                title,
                "본문",
                null,
                NotificationSource.ADMIN,
                "42",
                requestKey
        );
    }

    private static Notification stored(long id, NotificationCommand command, Instant createdAt) {
        Notification notification = Notification.create(
                command.botId(),
                command.targetType(),
                command.targetDiscordId(),
                command.category(),
                command.title(),
                command.body(),
                command.link(),
                command.source(),
                command.sourceRef(),
                command.requestKey(),
                createdAt
        );
        ReflectionTestUtils.setField(notification, "id", id);
        return notification;
    }

    @Test
    void keylessPublishSavesWithMillisecondUtcTimeAndRaisesCreatedEvent() {
        NotificationCommand command = guildCommand("점검 안내", null);
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> {
                    Notification saved = invocation.getArgument(0);
                    ReflectionTestUtils.setField(saved, "id", 7L);
                    return saved;
                });

        NotificationPublishResult result = service.publish(command);

        assertThat(result.created()).isTrue();
        assertThat(result.notification().id()).isEqualTo(7L);
        assertThat(result.notification().createdAt()).isEqualTo(NOW_MILLIS);
        ArgumentCaptor<NotificationCreatedEvent> event = ArgumentCaptor.forClass(NotificationCreatedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().message().botId()).isEqualTo(BOT);
        assertThat(event.getValue().message().notification().read()).isFalse();
    }

    @Test
    void userPublishFailsBeforeWritingWhenTargetIsNotActiveMember() {
        NotificationCommand command = NotificationCommand.internal(
                BOT,
                NotificationTargetType.USER,
                "123",
                NotificationCategory.POINT,
                "제목",
                "본문",
                null,
                "point"
        );
        when(discordUserRepository.findByBotIdAndDiscordID(BOT, "123")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.publish(command))
                .isInstanceOf(NotificationTargetNotFoundException.class);
        verify(notificationRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void userPublishChecksMembershipInTheSameGuild() {
        NotificationCommand command = NotificationCommand.internal(
                BOT,
                NotificationTargetType.USER,
                "123",
                NotificationCategory.POINT,
                "제목",
                "본문",
                null,
                "point"
        );
        when(discordUserRepository.findByBotIdAndDiscordID(BOT, "123"))
                .thenReturn(Optional.of(DiscordUser.of(BOT, "123", List.of())));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 8L);
            return saved;
        });

        assertThat(service.publish(command).notification().targetDiscordId()).isEqualTo("123");
    }

    @Test
    void firstKeyedPublishInsertsOnceAndRaisesEvent() {
        NotificationCommand command = guildCommand("공지", "req-1");
        Notification row = stored(11L, command, NOW_MILLIS);
        when(notificationRepository.findByBotIdAndSourceAndSourceRefAndRequestKey(
                BOT, NotificationSource.ADMIN, "42", "req-1"))
                .thenReturn(Optional.empty(), Optional.of(row));

        NotificationPublishResult result = service.publish(command);

        assertThat(result.created()).isTrue();
        assertThat(result.notification().id()).isEqualTo(11L);
        verify(notificationRepository).insertIfAbsent(
                eq(BOT), eq("GUILD"), eq(null), eq("ANNOUNCEMENT"), eq("공지"), eq("본문"), eq(null),
                eq("ADMIN"), eq("42"), eq("req-1"),
                eq(LocalDateTime.ofInstant(NOW_MILLIS, ZoneOffset.UTC))
        );
        verify(eventPublisher).publishEvent(any(NotificationCreatedEvent.class));
    }

    @Test
    void retransmittedSameRequestReturnsOriginalWithoutRebroadcast() {
        NotificationCommand command = guildCommand("공지", "req-1");
        Notification original = stored(11L, command, NOW_MILLIS.minusSeconds(5));
        when(notificationRepository.findByBotIdAndSourceAndSourceRefAndRequestKey(
                BOT, NotificationSource.ADMIN, "42", "req-1"))
                .thenReturn(Optional.of(original));

        NotificationPublishResult result = service.publish(command);

        assertThat(result.created()).isFalse();
        assertThat(result.notification().id()).isEqualTo(11L);
        verify(notificationRepository, never()).insertIfAbsent(
                anyString(), anyString(), any(), anyString(), anyString(), anyString(), any(),
                anyString(), anyString(), anyString(), any()
        );
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void sameRequestIdWithDifferentContentIsConflict() {
        Notification original = stored(11L, guildCommand("원래 공지", "req-1"), NOW_MILLIS);
        when(notificationRepository.findByBotIdAndSourceAndSourceRefAndRequestKey(
                BOT, NotificationSource.ADMIN, "42", "req-1"))
                .thenReturn(Optional.of(original));

        assertThatThrownBy(() -> service.publish(guildCommand("바뀐 공지", "req-1")))
                .isInstanceOf(NotificationRequestConflictException.class);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void concurrentDuplicateThatCommittedFirstIsTreatedAsReplay() {
        NotificationCommand command = guildCommand("공지", "req-1");
        Notification winner = stored(12L, command, NOW_MILLIS.minusMillis(3));
        when(notificationRepository.findByBotIdAndSourceAndSourceRefAndRequestKey(
                BOT, NotificationSource.ADMIN, "42", "req-1"))
                .thenReturn(Optional.empty(), Optional.of(winner));

        NotificationPublishResult result = service.publish(command);

        assertThat(result.created()).isFalse();
        assertThat(result.notification().id()).isEqualTo(12L);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void listUsesIdCursorAndMarksItemsUpToReadCursorAsRead() {
        NotificationCommand command = guildCommand("공지", null);
        when(readCursorRepository.findLastReadId(BOT, "123")).thenReturn(Optional.of(9L));
        when(notificationRepository.findVisibleBefore(
                eq(BOT), eq("123"), eq(NotificationTargetType.GUILD), eq(Long.MAX_VALUE), any(Pageable.class)))
                .thenReturn(List.of(
                        stored(10L, command, NOW_MILLIS),
                        stored(9L, command, NOW_MILLIS),
                        stored(8L, command, NOW_MILLIS)
                ));

        NotificationSliceResponse response = service.list(BOT, "123", null, 2);

        assertThat(response.items()).extracting(NotificationResponse::id).containsExactly(10L, 9L);
        assertThat(response.items()).extracting(NotificationResponse::read).containsExactly(false, true);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.nextBeforeId()).isEqualTo(9L);
        assertThat(response.lastReadId()).isEqualTo(9L);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(notificationRepository).findVisibleBefore(
                eq(BOT), eq("123"), eq(NotificationTargetType.GUILD), eq(Long.MAX_VALUE), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(3);
    }

    @Test
    void lastPageHasNoNextCursor() {
        when(readCursorRepository.findLastReadId(BOT, "123")).thenReturn(Optional.empty());
        when(notificationRepository.findVisibleBefore(
                eq(BOT), eq("123"), eq(NotificationTargetType.GUILD), eq(5L), any(Pageable.class)))
                .thenReturn(List.of(stored(4L, guildCommand("공지", null), NOW_MILLIS)));

        NotificationSliceResponse response = service.list(BOT, "123", 5L, 20);

        assertThat(response.hasNext()).isFalse();
        assertThat(response.nextBeforeId()).isNull();
        assertThat(response.items()).extracting(NotificationResponse::read).containsExactly(false);
    }

    @Test
    void unreadCountsOnlyNotificationsAfterReadCursor() {
        when(readCursorRepository.findLastReadId(BOT, "123")).thenReturn(Optional.of(3L));
        when(notificationRepository.findLatestVisibleId(BOT, "123", NotificationTargetType.GUILD))
                .thenReturn(Optional.of(7L));
        when(notificationRepository.countVisibleAfter(BOT, "123", NotificationTargetType.GUILD, 3L))
                .thenReturn(2L);

        NotificationUnreadResponse response = service.unread(BOT, "123");

        assertThat(response.unreadCount()).isEqualTo(2L);
        assertThat(response.latestId()).isEqualTo(7L);
        assertThat(response.lastReadId()).isEqualTo(3L);
    }

    @Test
    void unreadIsZeroWithoutAnyNotification() {
        when(readCursorRepository.findLastReadId(BOT, "123")).thenReturn(Optional.empty());
        when(notificationRepository.findLatestVisibleId(BOT, "123", NotificationTargetType.GUILD))
                .thenReturn(Optional.empty());

        NotificationUnreadResponse response = service.unread(BOT, "123");

        assertThat(response.unreadCount()).isZero();
        assertThat(response.latestId()).isNull();
        verify(notificationRepository, never()).countVisibleAfter(anyString(), anyString(), any(), anyLong());
    }

    @Test
    void markReadClampsCursorToLatestVisibleNotification() {
        when(notificationRepository.findLatestVisibleId(BOT, "123", NotificationTargetType.GUILD))
                .thenReturn(Optional.of(7L));
        when(readCursorRepository.findLastReadId(BOT, "123")).thenReturn(Optional.of(7L));

        NotificationUnreadResponse response = service.markRead(BOT, "123", Long.MAX_VALUE);

        verify(readCursorRepository).advance(
                BOT, "123", 7L, LocalDateTime.ofInstant(NOW, ZoneOffset.UTC));
        assertThat(response.unreadCount()).isZero();
    }

    @Test
    void markReadWithoutNotificationsDoesNotWriteCursor() {
        when(notificationRepository.findLatestVisibleId(BOT, "123", NotificationTargetType.GUILD))
                .thenReturn(Optional.empty());
        when(readCursorRepository.findLastReadId(BOT, "123")).thenReturn(Optional.empty());

        service.markRead(BOT, "123", 3L);

        verify(readCursorRepository, never()).advance(anyString(), anyString(), anyLong(), any());
    }
}
