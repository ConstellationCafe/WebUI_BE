package com.help.erpweb.domain.modules.competition.dto.request;

import java.time.LocalDateTime;
import java.util.List;

import com.help.erpweb.domain.modules.competition.service.CompetitionNotice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 대회 공지 입력. 시각은 한국 시간 wall time이며 {@code 2026-10-02T22:00} 형식으로 보낸다.
 * 줄바꿈, 예약 항목명, 봇 파서 표시 등 세부 규칙은 {@code CompetitionNoticeFormatter}가 검증한다.
 */
public record CompetitionNoticeRequest(
	@NotBlank @Size(max = 100) String title,
	@NotBlank String participantWay,
	@NotBlank String format,
	@NotNull LocalDateTime registrationStart,
	@NotNull LocalDateTime registrationEnd,
	@NotNull LocalDateTime eventStart,
	@Size(max = 10) List<@Valid @NotNull PrizeRequest> prizes,
	@Size(max = 10) List<@Valid @NotNull ExtraFieldRequest> extraFields
) {
	public record PrizeRequest(
		@NotBlank @Size(max = 30) String rank,
		@NotBlank String content
	) {
	}

	public record ExtraFieldRequest(
		@NotBlank @Size(max = 30) String key,
		@NotBlank String value
	) {
	}

	public CompetitionNotice toNotice() {
		return new CompetitionNotice(
			title,
			participantWay,
			format,
			registrationStart,
			registrationEnd,
			eventStart,
			prizes == null ? List.of() : prizes.stream()
				.map(prize -> new CompetitionNotice.Prize(prize.rank(), prize.content()))
				.toList(),
			extraFields == null ? List.of() : extraFields.stream()
				.map(field -> new CompetitionNotice.ExtraField(field.key(), field.value()))
				.toList());
	}
}
