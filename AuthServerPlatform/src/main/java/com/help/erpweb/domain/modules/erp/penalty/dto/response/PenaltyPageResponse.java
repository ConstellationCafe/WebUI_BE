package com.help.erpweb.domain.modules.erp.penalty.dto.response;

import java.util.List;

public record PenaltyPageResponse<T>(
		List<T> items,
		int page,
		int size,
		long totalElements,
		int totalPages,
		boolean hasNext
) {
	public static <T> PenaltyPageResponse<T> of(List<T> items, int page, int size, long total) {
		int pages = (int) ((total + size - 1) / size);
		return new PenaltyPageResponse<>(items, page, size, total, pages, page < pages);
	}
}
