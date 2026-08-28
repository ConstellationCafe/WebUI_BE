package com.help.erpweb.domain.academy.dto.response;

public record StatusPaginationResponse(
        int currentPage,
        int pageSize,
        int totalPages,
        long totalCount
) {
}