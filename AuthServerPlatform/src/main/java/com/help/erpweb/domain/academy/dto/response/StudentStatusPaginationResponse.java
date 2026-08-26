package com.help.erpweb.domain.academy.dto.response;

public record StudentStatusPaginationResponse(
        int currentPage,
        int pageSize,
        int totalPages,
        long totalCount
) {
}