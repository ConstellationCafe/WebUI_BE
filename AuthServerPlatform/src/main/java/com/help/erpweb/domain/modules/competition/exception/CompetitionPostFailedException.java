package com.help.erpweb.domain.modules.competition.exception;

/**
 * 디스코드에 공지를 게시하지 못했을 때. 502로 응답한다.
 * 원인(디스코드 응답 코드)은 로그에만 남기고, 봇 토큰이나 디스코드 응답 본문은 메시지에 넣지 않는다.
 */
public class CompetitionPostFailedException extends RuntimeException {
	public CompetitionPostFailedException(String message) {
		super(message);
	}

	public CompetitionPostFailedException(String message, Throwable cause) {
		super(message, cause);
	}
}
