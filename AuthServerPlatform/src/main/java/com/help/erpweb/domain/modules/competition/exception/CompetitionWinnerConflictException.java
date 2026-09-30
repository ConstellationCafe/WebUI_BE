package com.help.erpweb.domain.modules.competition.exception;

/** 같은 채팅방에서 같은 회원에게 같은 대회·버전 칭호가 이미 있을 때. 409로 응답한다. */
public class CompetitionWinnerConflictException extends RuntimeException {
	public CompetitionWinnerConflictException() {
		super("이미 같은 대회 우승 칭호가 부여된 회원입니다.");
	}
}
