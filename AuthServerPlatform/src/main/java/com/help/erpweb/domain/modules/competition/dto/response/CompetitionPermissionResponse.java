package com.help.erpweb.domain.modules.competition.dto.response;

/**
 * 현재 채팅방에서 내 대회 기능 권한. 화면이 대회 메뉴를 보일지 정하는 데 쓴다(최종 판단은 서버).
 *
 * @param manager 대회 공지 게시 등 대회 매니저 기능을 쓸 수 있는지(대회 매니저 역할 또는 서버장)
 */
public record CompetitionPermissionResponse(boolean manager) {
}
