package com.help.erpweb.domain.modules.competition.dto.response;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 부여된 대회 우승 칭호.
 *
 * @param winnerDisplayName 현재 채팅방에서의 서버 표시 이름. 알 수 없으면 null
 */
public record CompetitionWinnerResponse(
	String competitionName,
	String version,
	String winnerDiscordId,
	String winnerDisplayName,
	LocalDate acquisition
) {
	/** @deprecated ADR-0008 호환 필드. FE가 winnerDisplayName(으)로 전환한 뒤 제거한다. */
	@Deprecated
	@JsonProperty("winnerName")
	public String legacyWinnerName() {
		return winnerDisplayName;
	}
}
