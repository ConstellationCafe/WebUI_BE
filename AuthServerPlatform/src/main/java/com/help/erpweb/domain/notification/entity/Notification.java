package com.help.erpweb.domain.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 알림 원본(source of truth). 실시간 전달(SSE)은 놓칠 수 있으므로 사용자는 재접속할 때
 * 이 테이블을 기준으로 목록과 읽지 않은 개수를 다시 맞춘다(ADR-0004).
 * created_at은 UTC wall time으로 저장한다.
 */
@Entity
@Table(name = "Notification", schema = "Constellation_Network")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bot_id", nullable = false, length = 30)
    private String botId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 16)
    private NotificationTargetType targetType;

    @Column(name = "target_discord_id", length = 20)
    private String targetDiscordId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private NotificationCategory category;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "body", nullable = false, length = 1000)
    private String body;

    @Column(name = "link", length = 255)
    private String link;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 16)
    private NotificationSource source;

    @Column(name = "source_ref", nullable = false, length = 64)
    private String sourceRef;

    @Column(name = "request_key", length = 64)
    private String requestKey;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private Notification(
            String botId,
            NotificationTargetType targetType,
            String targetDiscordId,
            NotificationCategory category,
            String title,
            String body,
            String link,
            NotificationSource source,
            String sourceRef,
            String requestKey,
            Instant createdAt
    ) {
        this.botId = botId;
        this.targetType = targetType;
        this.targetDiscordId = targetDiscordId;
        this.category = category;
        this.title = title;
        this.body = body;
        this.link = link;
        this.source = source;
        this.sourceRef = sourceRef;
        this.requestKey = requestKey;
        this.createdAt = LocalDateTime.ofInstant(createdAt, ZoneOffset.UTC);
    }

    public static Notification create(
            String botId,
            NotificationTargetType targetType,
            String targetDiscordId,
            NotificationCategory category,
            String title,
            String body,
            String link,
            NotificationSource source,
            String sourceRef,
            String requestKey,
            Instant createdAt
    ) {
        return new Notification(
                botId,
                targetType,
                targetDiscordId,
                category,
                title,
                body,
                link,
                source,
                sourceRef,
                requestKey,
                createdAt
        );
    }

    public Instant createdInstant() {
        return createdAt.toInstant(ZoneOffset.UTC);
    }

    /** 이 알림을 해당 회원이 볼 수 있는지. 채팅방 스코프 확인은 조회 쿼리가 담당한다. */
    public boolean isVisibleTo(String discordId) {
        return targetType == NotificationTargetType.GUILD
                || Objects.equals(targetDiscordId, discordId);
    }

    /** 같은 멱등 키로 다시 들어온 요청이 처음 요청과 같은 내용인지 비교한다. */
    public boolean hasSameContent(
            NotificationTargetType otherTargetType,
            String otherTargetDiscordId,
            NotificationCategory otherCategory,
            String otherTitle,
            String otherBody,
            String otherLink
    ) {
        return targetType == otherTargetType
                && Objects.equals(targetDiscordId, otherTargetDiscordId)
                && category == otherCategory
                && Objects.equals(title, otherTitle)
                && Objects.equals(body, otherBody)
                && Objects.equals(link, otherLink);
    }
}
