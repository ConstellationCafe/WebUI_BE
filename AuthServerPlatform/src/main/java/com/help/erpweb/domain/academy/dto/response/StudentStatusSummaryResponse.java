package com.help.erpweb.domain.academy.dto.response;

public record StudentStatusSummaryResponse(
        long totalCount,
        long enrolledCount,
        long graduationCount,
        long expulsionCount,
        long withdrawalCount,
        long retirementCount,
        long disciplinaryCount
) {
}