package com.help.erpweb.domain.modules.competition.dto.response;

import java.util.List;

/** 칭호 부여 이력 페이지. 목록 응답 공통 형식(items, page, size, totalElements, totalPages, hasNext)을 따른다. */
public record CompetitionWinnerPageResponse(
	List<CompetitionWinnerResponse> items,
	int page,
	int size,
	long totalElements,
	int totalPages,
	boolean hasNext
) {
}
