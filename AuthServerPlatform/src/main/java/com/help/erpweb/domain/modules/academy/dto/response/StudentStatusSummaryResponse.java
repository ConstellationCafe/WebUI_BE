package com.help.erpweb.domain.modules.academy.dto.response;

public record StudentStatusSummaryResponse(
        long totalCount,
        long enrolledCount,
        long graduationCount,
        long expulsionCount,
        long withdrawalCount
) {
}
