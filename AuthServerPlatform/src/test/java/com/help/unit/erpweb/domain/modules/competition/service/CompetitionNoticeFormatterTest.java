package com.help.erpweb.domain.modules.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.help.erpweb.domain.modules.competition.exception.InvalidCompetitionNoticeException;

/**
 * 출력 형식은 봇 InfoExtractor가 읽는 형식과 같아야 한다.
 * (구현 시 Java 출력물을 봇의 실제 extractor로 파싱해 대회명·참가 방법·접수 마감·진행 시작이 일치함을 확인했다.)
 */
class CompetitionNoticeFormatterTest {
	private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 30, 12, 0);
	private static final LocalDateTime REG_START = LocalDateTime.of(2026, 10, 1, 0, 0);
	private static final LocalDateTime REG_END = LocalDateTime.of(2026, 10, 2, 0, 0);

	@Test
	void formatsSameShapeAsManualNotice() {
		String content = CompetitionNoticeFormatter.format(new CompetitionNotice(
			"미니미 Bo1 대회",
			"https://tonamel.com/competition/XtzgX",
			"싱글 엘리미네이션 Bo1",
			LocalDateTime.of(2026, 9, 29, 23, 0),
			LocalDateTime.of(2026, 10, 2, 21, 30),
			LocalDateTime.of(2026, 10, 2, 22, 0),
			List.of(new CompetitionNotice.Prize("1등", "피나치공 \"고구마피자L\" 기프티콘")),
			List.of(new CompetitionNotice.ExtraField(" 대회 규칙 ", " 덱 공개 없음 "))), NOW);

		assertThat(content).isEqualTo("""
			"미니미 Bo1 대회"가 개최되었습니다 !
			참가 방법 : https://tonamel.com/competition/XtzgX
			진행 형식 : 싱글 엘리미네이션 Bo1
			접수 기간 : 26년 9월 29일 23시 0분 ~ 26년 10월 2일 21시 30분
			진행 기간 : 26년 10월 2일 22시 ~ 종료시까지
			대회 규칙 : 덱 공개 없음
			• 우승 상품
			1등 상품 : 피나치공 "고구마피자L" 기프티콘""");
	}

	@Test
	void writesMinuteOnBothEndsSoParserDoesNotCarryStartMinuteToDeadline() {
		assertThat(CompetitionNoticeFormatter.formatRange(
			LocalDateTime.of(2026, 12, 30, 20, 45), LocalDateTime.of(2027, 1, 2, 21, 0)))
			.isEqualTo("26년 12월 30일 20시 45분 ~ 27년 1월 2일 21시 0분");
		assertThat(CompetitionNoticeFormatter.formatRange(REG_START, REG_END))
			.isEqualTo("26년 10월 1일 0시 ~ 26년 10월 2일 0시");
	}

	@Test
	void choosesSubjectParticleByFinalConsonant() {
		assertThat(CompetitionNoticeFormatter.subjectParticle("미니미 대회")).isEqualTo("가");
		assertThat(CompetitionNoticeFormatter.subjectParticle("연말 대전")).isEqualTo("이");
		assertThat(CompetitionNoticeFormatter.subjectParticle("시즌 3")).isEqualTo("이");
		assertThat(CompetitionNoticeFormatter.subjectParticle("시즌 2")).isEqualTo("가");
		assertThat(CompetitionNoticeFormatter.subjectParticle("Cup")).isEqualTo("가");
	}

	@Test
	void rejectsInputThatBreaksBotParser() {
		assertInvalid(notice("미니 \"Bo1\"", List.of()), "큰따옴표");
		assertInvalid(notice("a\nb", List.of()), "한 줄");
		assertInvalid(notice("a", List.of(new CompetitionNotice.ExtraField("접수 기간", "x"))), "예약된");
		assertInvalid(notice("a", List.of(new CompetitionNotice.ExtraField("비고", "진행 기간 : 11월"))), "진행 기간");
		assertInvalid(notice("a", List.of(new CompetitionNotice.ExtraField("a:b", "x"))), "':'");
		assertInvalid(notice("a", List.of(
			new CompetitionNotice.ExtraField("비고", "x"),
			new CompetitionNotice.ExtraField(" 비고", "y"))), "중복");
		assertInvalid(notice("a", List.of(new CompetitionNotice.ExtraField("k", "\"b\"가 개최되었습니다"))), "개최되었습니다");
	}

	@Test
	void rejectsInvalidSchedule() {
		assertInvalid(withDates(REG_END, REG_START, REG_END), "접수 시작보다");
		assertInvalid(withDates(REG_START, REG_END, REG_START), "진행 시작");
		assertInvalid(withDates(LocalDateTime.of(2026, 9, 1, 0, 0), NOW, REG_END), "이미 지났습니다");
		assertInvalid(withDates(null, REG_END, REG_END), "모두 입력");
	}

	@Test
	void rejectsContentLongerThanDiscordLimit() {
		assertInvalid(notice("a", List.of(
			new CompetitionNotice.ExtraField("k1", "x".repeat(1000)),
			new CompetitionNotice.ExtraField("k2", "x".repeat(1000)))), "2000자");
	}

	private static void assertInvalid(CompetitionNotice notice, String messageFragment) {
		assertThatThrownBy(() -> CompetitionNoticeFormatter.format(notice, NOW))
			.isInstanceOf(InvalidCompetitionNoticeException.class)
			.hasMessageContaining(messageFragment);
	}

	private static CompetitionNotice notice(String title, List<CompetitionNotice.ExtraField> extras) {
		return new CompetitionNotice(title, "https://tonamel.com/x", "Bo1", REG_START, REG_END, REG_END,
			List.of(), extras);
	}

	private static CompetitionNotice withDates(LocalDateTime start, LocalDateTime end, LocalDateTime event) {
		return new CompetitionNotice("a", "https://tonamel.com/x", "Bo1", start, end, event, List.of(), List.of());
	}
}
