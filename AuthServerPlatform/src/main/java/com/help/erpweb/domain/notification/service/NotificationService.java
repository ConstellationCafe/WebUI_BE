package com.help.erpweb.domain.notification.service;

import com.help.erpweb.domain.notification.dto.response.AdminNotificationPageResponse;
import com.help.erpweb.domain.notification.dto.response.AdminNotificationResponse;
import com.help.erpweb.domain.notification.dto.response.NotificationResponse;
import com.help.erpweb.domain.notification.dto.response.NotificationSliceResponse;
import com.help.erpweb.domain.notification.dto.response.NotificationUnreadResponse;
import com.help.erpweb.domain.notification.entity.Notification;
import com.help.erpweb.domain.notification.entity.NotificationTargetType;
import com.help.erpweb.domain.notification.exception.NotificationRequestConflictException;
import com.help.erpweb.domain.notification.exception.NotificationTargetNotFoundException;
import com.help.erpweb.domain.notification.realtime.NotificationMessage;
import com.help.erpweb.domain.notification.repository.NotificationReadCursorRepository;
import com.help.erpweb.domain.notification.repository.NotificationRepository;
import com.help.global.discord.identity.DiscordUserRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 알림 발행과 회원별 조회·읽음 처리.
 * <p>
 * 발행은 세 경로(관리자 API, 외부 API, 내부 함수 호출)가 모두 {@link #publish}로 모인다.
 * DB 저장이 원본이고, 실시간 전달은 커밋 이후 {@link NotificationCreatedEvent}로 이어진다.
 */
@Service
public class NotificationService implements NotificationPublisher {
    public static final int LIST_MAX_SIZE = 50;
    private static final String TX = "constellationTransactionManager";

    private final NotificationRepository notificationRepository;
    private final NotificationReadCursorRepository readCursorRepository;
    private final DiscordUserRepository discordUserRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;
    private final MeterRegistry meterRegistry;

    public NotificationService(
            NotificationRepository notificationRepository,
            NotificationReadCursorRepository readCursorRepository,
            DiscordUserRepository discordUserRepository,
            ApplicationEventPublisher eventPublisher,
            Clock clock,
            MeterRegistry meterRegistry
    ) {
        this.notificationRepository = notificationRepository;
        this.readCursorRepository = readCursorRepository;
        this.discordUserRepository = discordUserRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
        this.meterRegistry = meterRegistry;
    }

    /**
     * 요청 ID가 있으면 멱등하게 처리한다. 같은 ID·같은 내용의 재전송은 처음 알림을 그대로
     * 돌려주고 다시 전달하지 않으며, 같은 ID·다른 내용은 409로 거부한다.
     */
    @Override
    // 대상 없음·요청 충돌은 저장 전에 판단하므로 호출자 트랜잭션을 rollback-only로 만들지 않는다.
    // 내부 호출자는 이 예외를 잡고 자기 작업(예: 포인트 지급)을 계속 커밋할 수 있다.
    @Transactional(
            transactionManager = TX,
            noRollbackFor = {NotificationTargetNotFoundException.class, NotificationRequestConflictException.class}
    )
    public NotificationPublishResult publish(NotificationCommand command) {
        if (command.targetType() == NotificationTargetType.USER) {
            discordUserRepository.findByBotIdAndDiscordID(command.botId(), command.targetDiscordId())
                    .orElseThrow(NotificationTargetNotFoundException::new);
        }
        Instant now = clock.instant().truncatedTo(ChronoUnit.MILLIS);

        if (command.requestKey() == null) {
            Notification saved = notificationRepository.save(newNotification(command, now));
            return created(saved, command);
        }

        Optional<Notification> existing = findByKey(command);
        if (existing.isPresent()) {
            return replay(existing.get(), command);
        }
        LocalDateTime insertedAt = LocalDateTime.ofInstant(now, ZoneOffset.UTC);
        notificationRepository.insertIfAbsent(
                command.botId(),
                command.targetType().name(),
                command.targetDiscordId(),
                command.category().name(),
                command.title(),
                command.body(),
                command.link(),
                command.source().name(),
                command.sourceRef(),
                command.requestKey(),
                insertedAt
        );
        Notification stored = findByKey(command)
                .orElseThrow(() -> new IllegalStateException("저장한 알림을 다시 찾을 수 없습니다."));
        // 같은 키로 동시에 들어온 요청은 unique key 잠금으로 직렬화된다. 먼저 커밋된 쪽의
        // 행이 보이면(생성 시각이 다르면) 이번 요청은 재전송으로 취급한다.
        if (!stored.getCreatedAt().equals(insertedAt)) {
            return replay(stored, command);
        }
        return created(stored, command);
    }

    @Transactional(transactionManager = TX, readOnly = true)
    public NotificationSliceResponse list(String botId, String discordId, Long beforeId, int size) {
        long lastReadId = lastReadId(botId, discordId);
        long cursor = beforeId == null ? Long.MAX_VALUE : beforeId;
        List<Notification> rows = notificationRepository.findVisibleBefore(
                botId,
                discordId,
                NotificationTargetType.GUILD,
                cursor,
                PageRequest.of(0, size + 1)
        );
        boolean hasNext = rows.size() > size;
        List<Notification> page = hasNext ? rows.subList(0, size) : rows;
        List<NotificationResponse> items = page.stream()
                .map(notification -> NotificationResponse.of(notification, lastReadId))
                .toList();
        Long nextBeforeId = hasNext ? page.get(page.size() - 1).getId() : null;
        return new NotificationSliceResponse(items, hasNext, nextBeforeId, lastReadId);
    }

    @Transactional(transactionManager = TX, readOnly = true)
    public NotificationUnreadResponse unread(String botId, String discordId) {
        long lastReadId = lastReadId(botId, discordId);
        Long latestId = notificationRepository
                .findLatestVisibleId(botId, discordId, NotificationTargetType.GUILD)
                .orElse(null);
        long unreadCount = latestId == null || latestId <= lastReadId
                ? 0L
                : notificationRepository.countVisibleAfter(
                        botId,
                        discordId,
                        NotificationTargetType.GUILD,
                        lastReadId
                );
        return new NotificationUnreadResponse(unreadCount, latestId, lastReadId);
    }

    /**
     * 읽음 위치를 {@code lastReadId}까지 앞으로 옮긴다. 회원이 볼 수 있는 최신 알림 ID보다
     * 큰 값은 그 ID로 낮춰, 아직 발행되지 않은 알림까지 읽음 처리되지 않게 한다.
     */
    @Transactional(transactionManager = TX)
    public NotificationUnreadResponse markRead(String botId, String discordId, long lastReadId) {
        Optional<Long> latestId = notificationRepository
                .findLatestVisibleId(botId, discordId, NotificationTargetType.GUILD);
        if (latestId.isPresent()) {
            long target = Math.min(lastReadId, latestId.get());
            readCursorRepository.advance(
                    botId,
                    discordId,
                    target,
                    LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC)
            );
        }
        return unread(botId, discordId);
    }

    @Transactional(transactionManager = TX, readOnly = true)
    public AdminNotificationPageResponse adminList(String botId, int page, int size) {
        Page<Notification> result = notificationRepository.findByBotIdOrderByIdDesc(
                botId,
                PageRequest.of(page - 1, size)
        );
        return new AdminNotificationPageResponse(
                result.getContent().stream().map(AdminNotificationResponse::of).toList(),
                page,
                size,
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext()
        );
    }

    private NotificationPublishResult created(Notification notification, NotificationCommand command) {
        eventPublisher.publishEvent(new NotificationCreatedEvent(NotificationMessage.from(notification)));
        Counter.builder("notification.published")
                .tag("source", command.source().name())
                .tag("target", command.targetType().name())
                .register(meterRegistry)
                .increment();
        return new NotificationPublishResult(AdminNotificationResponse.of(notification), true);
    }

    private NotificationPublishResult replay(Notification notification, NotificationCommand command) {
        boolean sameContent = notification.hasSameContent(
                command.targetType(),
                command.targetDiscordId(),
                command.category(),
                command.title(),
                command.body(),
                command.link()
        );
        if (!sameContent) {
            throw new NotificationRequestConflictException();
        }
        return new NotificationPublishResult(AdminNotificationResponse.of(notification), false);
    }

    private Optional<Notification> findByKey(NotificationCommand command) {
        return notificationRepository.findByBotIdAndSourceAndSourceRefAndRequestKey(
                command.botId(),
                command.source(),
                command.sourceRef(),
                command.requestKey()
        );
    }

    private long lastReadId(String botId, String discordId) {
        return readCursorRepository.findLastReadId(botId, discordId).orElse(0L);
    }

    private static Notification newNotification(NotificationCommand command, Instant now) {
        return Notification.create(
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
                now
        );
    }
}
