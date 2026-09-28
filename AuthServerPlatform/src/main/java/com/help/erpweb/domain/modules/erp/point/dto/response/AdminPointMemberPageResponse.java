package com.help.erpweb.domain.modules.erp.point.dto.response;

import java.util.List;

public record AdminPointMemberPageResponse(
        List<AdminPointMemberResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
}
