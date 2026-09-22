package com.help.erpweb.domain.academy.dto.response;

import java.util.List;

public record StudentStatusListResponse(
        List<StatusItemResponse<StudentResponse>> items,
        StudentStatusSummaryResponse summary,
        StatusPaginationResponse pagination
) {

    public StudentStatusListResponse {

        items = items != null
                ? items
                : List.of();

        summary = summary != null
                ? summary
                : new StudentStatusSummaryResponse(
                0,
                0,
                0,
                0,
                0
        );

        pagination = pagination != null
                ? pagination
                : new StatusPaginationResponse(
                1,
                20,
                0,
                0
        );
    }
}
