package com.help.erpweb.domain.modules.competition.exception;

/** 우승자가 현재 채팅방의 재적 회원이 아닐 때. 404로 응답한다. */
public class CompetitionMemberNotFoundException extends RuntimeException {
	public CompetitionMemberNotFoundException() {
		super("현재 채팅방의 재적 회원이 아닙니다. Discord ID를 확인해 주세요.");
	}
}
