package com.help.erpweb.domain.modules.competition.service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 대회 공지 한 건의 입력값. 시각은 모두 한국 시간(Asia/Seoul) 기준 wall time이다.
 * 진행 종료 시각은 받지 않는다. 봇 파서가 진행 기간 줄에서 날짜를 하나만 읽기 때문에
 * 항상 "시작 ~ 종료시까지" 형식으로 게시한다.
 */
public record CompetitionNotice(
	String title,
	String participantWay,
	String format,
	LocalDateTime registrationStart,
	LocalDateTime registrationEnd,
	LocalDateTime eventStart,
	List<Prize> prizes,
	List<ExtraField> extraFields
) {
	public CompetitionNotice {
		prizes = prizes == null ? List.of() : List.copyOf(prizes);
		extraFields = extraFields == null ? List.of() : List.copyOf(extraFields);
	}

	/** 우승 상품 한 줄. 예: rank="1등", content="치킨 기프티콘" -> "1등 상품 : 치킨 기프티콘" */
	public record Prize(String rank, String content) {
	}

	/** 관리자가 추가한 입력란 한 줄. 예: key="대회 규칙", value="덱 공개 없음" */
	public record ExtraField(String key, String value) {
	}
}
