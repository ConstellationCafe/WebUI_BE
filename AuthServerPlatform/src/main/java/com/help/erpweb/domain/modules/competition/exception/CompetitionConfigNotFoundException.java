package com.help.erpweb.domain.modules.competition.exception;

/**
 * 현재 채팅방(botId)에 대회 게시판 설정이나 봇 실행 환경이 없을 때. 404로 응답한다.
 * 설정은 별도 설정 사이트에서 관리하므로, 관리자에게 설정 확인을 안내한다.
 */
public class CompetitionConfigNotFoundException extends RuntimeException {
	public CompetitionConfigNotFoundException(String message) {
		super(message);
	}
}
