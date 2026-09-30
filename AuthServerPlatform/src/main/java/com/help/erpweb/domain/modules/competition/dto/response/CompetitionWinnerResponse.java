package com.help.erpweb.domain.modules.competition.dto.response;

import java.time.LocalDate;

/**
 * 부여된 대회 우승 칭호.
 *
 * @param winnerName 현재 채팅방에서의 회원 이름. 알 수 없으면 null
 */
public record CompetitionWinnerResponse(
	String competitionName,
	String version,
	String winnerDiscordId,
	String winnerName,
	LocalDate acquisition
) {
}
