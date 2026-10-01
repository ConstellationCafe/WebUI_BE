package com.help.erpweb.domain.modules.competition.dto.response;

/**
 * 게시 결과. 대회 등록(스케줄러)과 WebUI 알림은 봇이 이 글을 감지한 뒤 처리하므로 여기에 포함되지 않는다.
 *
 * @param messageUrl 디스코드 공지글 바로가기
 */
public record CompetitionPostResponse(
	String boardKey,
	String channelId,
	String messageId,
	String messageUrl,
	String content
) {
}
