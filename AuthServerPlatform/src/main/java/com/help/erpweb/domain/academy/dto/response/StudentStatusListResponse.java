package com.help.erpweb.domain.academy.dto.response;

import java.util.List;

public record StudentStatusListResponse(
        List<StudentStatusItemResponse> items,
        StudentStatusSummaryResponse summary,
        StudentStatusPaginationResponse pagination
) {
}