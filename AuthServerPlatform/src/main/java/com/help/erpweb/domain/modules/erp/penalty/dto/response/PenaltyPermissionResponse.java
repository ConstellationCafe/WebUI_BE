package com.help.erpweb.domain.modules.erp.penalty.dto.response;

/**
 * 현재 채팅방에서 내 벌점 관리 권한. 화면이 벌점 관리 메뉴를 보일지 정하는 데 쓴다(최종 판단은 서버).
 *
 * @param manager 벌점 부여·취소·조회를 할 수 있는지(운영 매니저·운영 본부원 역할 또는 서버장)
 */
public record PenaltyPermissionResponse(boolean manager) {
}
