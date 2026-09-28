package com.help.erpweb.domain.modules.erp.penalty.dto.response;

import java.time.Instant;

import com.help.erpweb.domain.modules.erp.penalty.entity.PenaltyLog;
import com.help.erpweb.domain.modules.erp.penalty.entity.PenaltyStatus;

public record PenaltyHistoryItemResponse(
		long penaltyId,
		String channelId,
		String channelName,
		String targetDiscordId,
		String targetUsername,
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
	public static PenaltyHistoryItemResponse of(PenaltyLog log, long score) {
		return new PenaltyHistoryItemResponse(
				log.getId(), log.getChannelId(), log.getChannelName(), log.getTargetDiscordId(),
				log.getTargetUsername(), log.getReason(), log.getScore(), log.getIssuerDiscordId(),
				log.occurredInstant(), log.createdInstant(), log.getStatus(), score,
				log.getCanceledByDiscordId(), log.canceledInstant(), log.getCancellationReason()
		);
	}
}
