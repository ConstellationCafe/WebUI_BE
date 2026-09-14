package com.help.erpweb.domain.academy.dto.response;

import java.util.List;

public record TeacherStatusListResponse(
        List<StatusItemResponse<TeacherResponse>> items,
        TeacherStatusSummaryResponse summary,
        StatusPaginationResponse pagination
) {

    public TeacherStatusListResponse {

        items = items != null
                ? items
                : List.of();

        summary = summary != null
                ? summary
                : new TeacherStatusSummaryResponse(
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