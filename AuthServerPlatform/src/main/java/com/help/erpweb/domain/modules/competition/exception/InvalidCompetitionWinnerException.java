package com.help.erpweb.domain.modules.competition.exception;

/** 우승 칭호 부여 입력이 잘못됐을 때. 400으로 응답한다. 메시지는 화면에 그대로 보여 줄 수 있게 작성한다. */
public class InvalidCompetitionWinnerException extends IllegalArgumentException {
	public InvalidCompetitionWinnerException(String message) {
		super(message);
	}
}
