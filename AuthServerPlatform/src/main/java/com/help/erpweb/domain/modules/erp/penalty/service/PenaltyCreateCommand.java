package com.help.erpweb.domain.modules.erp.penalty.service;

import java.time.Instant;

public record PenaltyCreateCommand(
		String requestId,
		String targetDiscordId,
		String channelId,
		String channelName,
		String reason,
		int score,
		Instant occurredAt,
		String issuerDiscordId
) {
}
