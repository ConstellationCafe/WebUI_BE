package com.help.erpweb.domain.modules.erp.penalty.dto.response;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PenaltyMemberRankResponse(
		String discordId,
		String displayName,
		long cumulativeScore30d,
		long penaltyCount30d,
		Instant lastOccurredAt
) {
	/** @deprecated ADR-0008 호환 필드. FE가 displayName(으)로 전환한 뒤 제거한다. */
	@Deprecated
	@JsonProperty("username")
	public String legacyUsername() {
		return displayName;
	}
}
