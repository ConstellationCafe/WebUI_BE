package com.help.erpweb.domain.modules.competition.dto.request;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.help.erpweb.domain.modules.competition.service.CompetitionNotice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 대회 공지 입력. 시각은 공통 규칙대로 UTC ISO-8601({@code 2026-10-02T13:00:00Z})로 받고,
 * 게시글에는 {@link CompetitionNotice#ZONE}(한국 시간)으로 바꿔 분 단위로 적는다.
 * 줄바꿈, 예약 항목명, 봇 파서 표시 등 세부 규칙은 {@code CompetitionNoticeFormatter}가 검증한다.
 */
public record CompetitionNoticeRequest(
	@NotBlank @Size(max = 100) String title,
	@NotBlank String participantWay,
	@NotBlank String format,
	@NotNull Instant registrationStart,
	@NotNull Instant registrationEnd,
	@NotNull Instant eventStart,
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
			toNoticeTime(registrationStart),
			toNoticeTime(registrationEnd),
			toNoticeTime(eventStart),
			prizes == null ? List.of() : prizes.stream()
				.map(prize -> new CompetitionNotice.Prize(prize.rank(), prize.content()))
				.toList(),
			extraFields == null ? List.of() : extraFields.stream()
				.map(field -> new CompetitionNotice.ExtraField(field.key(), field.value()))
				.toList());
	}

	/** 게시글은 분 단위로만 적으므로 초 이하를 버려, 검증과 게시 내용이 같은 시각을 보게 한다. */
	private static LocalDateTime toNoticeTime(Instant instant) {
		return instant == null
			? null
			: LocalDateTime.ofInstant(instant, CompetitionNotice.ZONE).truncatedTo(ChronoUnit.MINUTES);
	}
}
