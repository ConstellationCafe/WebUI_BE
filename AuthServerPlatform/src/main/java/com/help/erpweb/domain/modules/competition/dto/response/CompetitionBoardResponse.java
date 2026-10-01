package com.help.erpweb.domain.modules.competition.dto.response;

/**
 * 현재 채팅방에서 고를 수 있는 대회 게시판.
 *
 * @param key      설정의 게시판 키(inner_board, outer_board 등). 게시 요청에 이 값을 보낸다.
 * @param name     디스코드 채널 이름. 조회에 실패하면 key를 대신 쓴다.
 * @param joinable 참가 이모지·참가자 역할·대회방이 자동으로 만들어지는 게시판인지 (현재 inner_board만)
 */
public record CompetitionBoardResponse(
	String key,
	String channelId,
	String name,
	boolean joinable
) {
}
