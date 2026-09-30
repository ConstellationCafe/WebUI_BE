package com.help.erpweb.domain.modules.competition.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * 대회 공지 게시 요청.
 *
 * @param requestId 화면에서 게시 시도마다 만드는 값(UUID 권장). 네트워크 오류로 몇 분 안에 다시 보내도
 *                  디스코드에 글이 두 번 올라가지 않게 한다. 다시 보낼 때는 같은 값을 써야 한다.
 * @param boardKey  {@code GET /api/admin/competitions/boards}가 돌려준 게시판 키
 */
public record CompetitionCreateRequest(
	@NotBlank @Pattern(regexp = "[A-Za-z0-9._:-]{1,64}") String requestId,
	@NotBlank @Pattern(regexp = "[A-Za-z0-9_]{1,30}") String boardKey,
	@NotNull @Valid CompetitionNoticeRequest notice
) {
}
