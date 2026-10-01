package com.help.erpweb.domain.modules.competition.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 대회 우승 칭호 부여 요청.
 *
 * @param version         게임 버전 값({@code GameVersion.value()}, 예: {@code s2})
 * @param winnerDiscordId 현재 채팅방 재적 회원의 Discord ID
 * @param acquisition     대회 개최 날짜(한국 날짜, {@code 2026-10-02}). 시각이 아니라 날짜라 UTC로 바꾸지 않는다.
 */
public record CompetitionWinnerGrantRequest(
	@NotBlank @Size(max = 100) String competitionName,
	@NotBlank @Size(max = 2) String version,
	@NotBlank @Pattern(regexp = "[0-9]{1,20}") String winnerDiscordId,
	@NotNull LocalDate acquisition
) {
}
