package com.help.erpweb.domain.modules.competition.exception;

/**
 * 대회 공지 입력이 봇 파서 규칙이나 게시 제약을 어겼을 때. 400으로 응답한다.
 * 메시지는 관리자 화면에 그대로 보여 줄 수 있게 작성한다.
 */
public class InvalidCompetitionNoticeException extends IllegalArgumentException {
	public InvalidCompetitionNoticeException(String message) {
		super(message);
	}
}
