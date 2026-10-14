package com.help.erpweb.domain.modules.erp.penalty.dto.response;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.help.erpweb.domain.modules.erp.penalty.entity.PenaltyLog;
import com.help.erpweb.domain.modules.erp.penalty.entity.PenaltyStatus;

public record PenaltyHistoryItemResponse(
		long penaltyId,
		String channelId,
		String channelName,
		String targetDiscordId,
		String targetDisplayName,
		String reason,
		int score,
		String issuerDiscordId,
		Instant occurredAt,
		Instant createdAt,
		PenaltyStatus status,
		long targetCumulativeScore30d,
		String canceledByDiscordId,
		Instant canceledAt,
		String cancellationReason
) {
	/** @deprecated ADR-0008 호환 필드. FE가 targetDisplayName(으)로 전환한 뒤 제거한다. */
	@Deprecated
	@JsonProperty("targetUsername")
	public String legacyTargetUsername() {
		return targetDisplayName;
	}

	public static PenaltyHistoryItemResponse of(PenaltyLog log, long score) {
		return new PenaltyHistoryItemResponse(
				log.getId(), log.getChannelId(), log.getChannelName(), log.getTargetDiscordId(),
				log.getTargetDisplayName(), log.getReason(), log.getScore(), log.getIssuerDiscordId(),
				log.occurredInstant(), log.createdInstant(), log.getStatus(), score,
				log.getCanceledByDiscordId(), log.canceledInstant(), log.getCancellationReason()
		);
	}
}
