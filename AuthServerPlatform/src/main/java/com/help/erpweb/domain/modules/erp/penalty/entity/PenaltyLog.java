package com.help.erpweb.domain.modules.erp.penalty.entity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "PenaltyLog", schema = "Constellation_Network")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PenaltyLog {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "bot_id", nullable = false, length = 30)
	private String botId;
	@Column(name = "target_discord_id", nullable = false, length = 20)
	private String targetDiscordId;
	@Column(name = "target_username", nullable = false, length = 100)
	private String targetUsername;
	@Column(name = "channel_id", nullable = false, length = 20)
	private String channelId;
	@Column(name = "channel_name", length = 100)
	private String channelName;
	@Column(name = "reason", nullable = false, length = 255)
	private String reason;
	@Column(name = "score", nullable = false)
	private int score;
	@Column(name = "issuer_discord_id", nullable = false, length = 20)
	private String issuerDiscordId;
	@Column(name = "occurred_at", nullable = false)
	private LocalDateTime occurredAt;
	@Column(name = "requested_occurred_at")
	private LocalDateTime requestedOccurredAt;
	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 16)
	private PenaltyStatus status;
	@Column(name = "request_id", nullable = false, length = 36)
	private String requestId;
	@Column(name = "canceled_by_discord_id", length = 20)
	private String canceledByDiscordId;
	@Column(name = "canceled_at")
	private LocalDateTime canceledAt;
	@Column(name = "cancellation_reason", length = 255)
	private String cancellationReason;

	public Instant occurredInstant() {
		return occurredAt.toInstant(ZoneOffset.UTC);
	}

	public Instant createdInstant() {
		return createdAt.toInstant(ZoneOffset.UTC);
	}

	public Instant canceledInstant() {
		return canceledAt == null ? null : canceledAt.toInstant(ZoneOffset.UTC);
	}

	public Instant requestedOccurredInstant() {
		return requestedOccurredAt == null ? null : requestedOccurredAt.toInstant(ZoneOffset.UTC);
	}

	public void cancel(String issuer, Instant at, String reason) {
		if (status == PenaltyStatus.ACTIVE) {
			status = PenaltyStatus.CANCELED;
			canceledByDiscordId = issuer;
			canceledAt = LocalDateTime.ofInstant(at, ZoneOffset.UTC);
			cancellationReason = reason;
		}
	}
}
