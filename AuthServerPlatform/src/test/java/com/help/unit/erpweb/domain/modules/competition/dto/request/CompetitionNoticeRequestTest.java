package com.help.erpweb.domain.modules.competition.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.help.erpweb.domain.modules.competition.service.CompetitionNotice;

class CompetitionNoticeRequestTest {
	@Test
	void convertsUtcInstantsToKoreanMinutesForNotice() {
		CompetitionNoticeRequest request = new CompetitionNoticeRequest(
			"대회", "https://tonamel.com/x", "Bo1",
			Instant.parse("2026-09-29T14:00:00Z"),
			Instant.parse("2026-10-02T12:30:59.999Z"),
			Instant.parse("2026-10-02T13:00:00Z"),
			List.of(new CompetitionNoticeRequest.PrizeRequest("1등", "치킨")),
			null);

		CompetitionNotice notice = request.toNotice();

		assertThat(notice.registrationStart()).isEqualTo(LocalDateTime.of(2026, 9, 29, 23, 0));
		assertThat(notice.registrationEnd()).isEqualTo(LocalDateTime.of(2026, 10, 2, 21, 30));
		assertThat(notice.eventStart()).isEqualTo(LocalDateTime.of(2026, 10, 2, 22, 0));
		assertThat(notice.prizes()).containsExactly(new CompetitionNotice.Prize("1등", "치킨"));
		assertThat(notice.extraFields()).isEmpty();
	}
}
