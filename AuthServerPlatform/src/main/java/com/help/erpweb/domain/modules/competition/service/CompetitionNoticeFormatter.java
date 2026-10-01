package com.help.erpweb.domain.modules.competition.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.help.erpweb.domain.modules.competition.exception.InvalidCompetitionNoticeException;

/**
 * 대회 공지 입력을 빗자루 봇이 인식하는 평문으로 조립한다.
 *
 * <p>봇의 {@code InfoExtractor}(ModularDiscordBot
 * modules/network_operations/add_on/competition/service/logic/extractor.py)는 다음 규칙으로 글을 읽는다.
 * 이 클래스의 검증은 그 규칙을 어기는 입력을 게시 전에 막기 위한 것이다.
 * <ul>
 *     <li>첫 번째 {@code "대회명"(이|가) 개최되었습니다} 에서 대회명을 읽는다. 대회명에 큰따옴표가 있으면 깨진다.</li>
 *     <li>{@code 참가 방법 : }, {@code 접수 기간 : }, {@code 진행 기간 : } 이 처음 나오는 줄의 나머지를 값으로 읽는다.
 *     그래서 이 표시가 다른 입력란에 먼저 나오면 안 된다.</li>
 *     <li>날짜는 한 줄에서 년/월/일/시/분 토큰을 차례로 덮어쓴다. 접수 기간 줄에서는 마지막 값(마감)이 남고,
 *     진행 기간 줄에서는 날짜를 하나만 얻는다. 연도가 없으면 올해로 채우므로 항상 2자리 연도를 쓴다.</li>
 * </ul>
 */
public final class CompetitionNoticeFormatter {
	public static final int MAX_CONTENT_LENGTH = 2000; // Discord 메시지 길이 제한
	public static final int MAX_TITLE_LENGTH = 100;
	public static final int MAX_KEY_LENGTH = 30;
	public static final int MAX_EXTRA_FIELDS = 10;
	public static final int MAX_PRIZES = 10;

	/** 봇 파서가 값을 찾는 표시. 입력값 어디에도 들어가면 안 된다. */
	static final List<String> PARSER_MARKERS = List.of("참가 방법 : ", "접수 기간 : ", "진행 기간 : ");
	/** 고정 입력란과 같은 이름이라 추가 입력란 키로 쓸 수 없는 값 */
	static final Set<String> RESERVED_KEYS = Set.of("참가 방법", "진행 형식", "접수 기간", "진행 기간", "우승 상품");

	private CompetitionNoticeFormatter() {
	}

	/**
	 * 입력을 검증하고 평문을 만든다.
	 *
	 * @param now 한국 시간 기준 현재 시각. 접수 마감이 이미 지난 공지를 막는 데 쓴다.
	 * @throws InvalidCompetitionNoticeException 입력이 규칙을 어긴 경우
	 */
	public static String format(CompetitionNotice input, LocalDateTime now) {
		validate(input, now);
		final CompetitionNotice notice = strip(input);

		final StringBuilder content = new StringBuilder()
			.append('"').append(notice.title()).append('"').append(subjectParticle(notice.title()))
			.append(" 개최되었습니다 !\n")
			.append("참가 방법 : ").append(notice.participantWay()).append('\n')
			.append("진행 형식 : ").append(notice.format()).append('\n')
			.append("접수 기간 : ").append(formatRange(notice.registrationStart(), notice.registrationEnd()))
			.append('\n')
			.append("진행 기간 : ").append(formatDateTime(notice.eventStart(), notice.eventStart().getMinute() != 0))
			.append(" ~ 종료시까지");

		for (CompetitionNotice.ExtraField field : notice.extraFields()) {
			content.append('\n').append(field.key()).append(" : ").append(field.value());
		}
		if (!notice.prizes().isEmpty()) {
			content.append("\n• 우승 상품");
			for (CompetitionNotice.Prize prize : notice.prizes()) {
				content.append('\n').append(prize.rank()).append(" 상품 : ").append(prize.content());
			}
		}

		if (content.length() > MAX_CONTENT_LENGTH) {
			throw new InvalidCompetitionNoticeException(
				"공지 전체 길이가 " + content.length() + "자입니다. 디스코드 제한인 "
					+ MAX_CONTENT_LENGTH + "자 이하로 줄여 주세요.");
		}
		return content.toString();
	}

	private static void validate(CompetitionNotice notice, LocalDateTime now) {
		if (notice == null) {
			throw new InvalidCompetitionNoticeException("대회 정보가 없습니다.");
		}
		requireLine("제목", notice.title(), MAX_TITLE_LENGTH);
		if (notice.title().contains("\"")) {
			throw new InvalidCompetitionNoticeException("제목에는 큰따옴표(\")를 넣을 수 없습니다.");
		}
		requireLine("참가 방법", notice.participantWay(), MAX_CONTENT_LENGTH);
		requireLine("진행 형식", notice.format(), MAX_CONTENT_LENGTH);

		if (notice.registrationStart() == null || notice.registrationEnd() == null || notice.eventStart() == null) {
			throw new InvalidCompetitionNoticeException("접수 시작, 접수 마감, 진행 시작 시각을 모두 입력해 주세요.");
		}
		if (!notice.registrationStart().isBefore(notice.registrationEnd())) {
			throw new InvalidCompetitionNoticeException("접수 마감은 접수 시작보다 늦어야 합니다.");
		}
		if (notice.eventStart().isBefore(notice.registrationEnd())) {
			throw new InvalidCompetitionNoticeException("진행 시작은 접수 마감과 같거나 늦어야 합니다.");
		}
		if (now != null && !notice.registrationEnd().isAfter(now)) {
			throw new InvalidCompetitionNoticeException("접수 마감이 이미 지났습니다.");
		}

		if (notice.extraFields().size() > MAX_EXTRA_FIELDS) {
			throw new InvalidCompetitionNoticeException("추가 입력란은 " + MAX_EXTRA_FIELDS + "개까지 넣을 수 있습니다.");
		}
		final Set<String> keys = new HashSet<>();
		for (CompetitionNotice.ExtraField field : notice.extraFields()) {
			if (field == null) {
				throw new InvalidCompetitionNoticeException("비어 있는 추가 입력란이 있습니다.");
			}
			requireLine("추가 입력란 이름", field.key(), MAX_KEY_LENGTH);
			final String key = field.key().strip();
			if (RESERVED_KEYS.contains(key)) {
				throw new InvalidCompetitionNoticeException("'" + key + "'은(는) 예약된 항목명이라 추가 입력란 이름으로 쓸 수 없습니다.");
			}
			if (key.contains(":") || key.startsWith("•")) {
				throw new InvalidCompetitionNoticeException("추가 입력란 이름에는 ':'를 넣거나 '•'로 시작할 수 없습니다.");
			}
			if (!keys.add(key)) {
				throw new InvalidCompetitionNoticeException("추가 입력란 이름 '" + key + "'이(가) 중복됩니다.");
			}
			requireLine("'" + key + "' 값", field.value(), MAX_CONTENT_LENGTH);
		}

		if (notice.prizes().size() > MAX_PRIZES) {
			throw new InvalidCompetitionNoticeException("우승 상품은 " + MAX_PRIZES + "개까지 넣을 수 있습니다.");
		}
		for (CompetitionNotice.Prize prize : notice.prizes()) {
			if (prize == null) {
				throw new InvalidCompetitionNoticeException("비어 있는 우승 상품이 있습니다.");
			}
			requireLine("상품 순위", prize.rank(), MAX_KEY_LENGTH);
			if (prize.rank().contains(":")) {
				throw new InvalidCompetitionNoticeException("상품 순위에는 ':'를 넣을 수 없습니다.");
			}
			requireLine("'" + prize.rank() + "' 상품", prize.content(), MAX_CONTENT_LENGTH);
		}
	}

	/** 비어 있지 않은 한 줄 값인지, 봇 파서 표시를 포함하지 않는지 확인한다. */
	private static void requireLine(String label, String value, int maxLength) {
		if (value == null || value.isBlank()) {
			throw new InvalidCompetitionNoticeException(label + "을(를) 입력해 주세요.");
		}
		if (value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
			throw new InvalidCompetitionNoticeException(label + "은(는) 한 줄로 입력해 주세요.");
		}
		if (value.length() > maxLength) {
			throw new InvalidCompetitionNoticeException(label + "은(는) " + maxLength + "자 이하로 입력해 주세요.");
		}
		for (String marker : PARSER_MARKERS) {
			if (value.contains(marker)) {
				throw new InvalidCompetitionNoticeException(
					label + "에 '" + marker.strip() + "' 표시를 넣을 수 없습니다. 봇이 공지를 잘못 읽게 됩니다.");
			}
		}
		if (value.contains("개최되었습니다")) {
			throw new InvalidCompetitionNoticeException(label + "에 '개최되었습니다'를 넣을 수 없습니다.");
		}
	}

	/** 검증을 통과한 입력의 앞뒤 공백을 정리한다. */
	private static CompetitionNotice strip(CompetitionNotice notice) {
		return new CompetitionNotice(
			notice.title().strip(),
			notice.participantWay().strip(),
			notice.format().strip(),
			notice.registrationStart(),
			notice.registrationEnd(),
			notice.eventStart(),
			notice.prizes().stream()
				.map(prize -> new CompetitionNotice.Prize(prize.rank().strip(), prize.content().strip()))
				.toList(),
			notice.extraFields().stream()
				.map(field -> new CompetitionNotice.ExtraField(field.key().strip(), field.value().strip()))
				.toList());
	}

	/** 같은 줄의 뒤 날짜가 앞 날짜의 분 값을 물려받지 않도록, 한쪽이라도 분이 있으면 둘 다 분을 쓴다. */
	static String formatRange(LocalDateTime start, LocalDateTime end) {
		final boolean withMinute = start.getMinute() != 0 || end.getMinute() != 0;
		return formatDateTime(start, withMinute) + " ~ " + formatDateTime(end, withMinute);
	}

	static String formatDateTime(LocalDateTime time, boolean withMinute) {
		final String base = String.format("%02d년 %d월 %d일 %d시",
			time.getYear() % 100, time.getMonthValue(), time.getDayOfMonth(), time.getHour());
		return withMinute ? base + " " + time.getMinute() + "분" : base;
	}

	/** 제목 끝 글자의 받침에 맞춰 이/가를 고른다. 봇 파서는 둘 다 인식한다. */
	static String subjectParticle(String title) {
		final char last = title.strip().charAt(title.strip().length() - 1);
		if (last >= '가' && last <= '힣') {
			return (last - '가') % 28 == 0 ? "가" : "이";
		}
		if (Character.isDigit(last)) {
			// 0(영) 1(일) 3(삼) 6(육) 7(칠) 8(팔)은 받침이 있다
			return "013678".indexOf(last) >= 0 ? "이" : "가";
		}
		return "가";
	}
}
