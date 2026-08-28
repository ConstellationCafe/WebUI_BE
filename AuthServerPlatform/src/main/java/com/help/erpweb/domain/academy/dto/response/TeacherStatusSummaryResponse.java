package com.help.erpweb.domain.academy.dto.response;

public record TeacherStatusSummaryResponse(
        long totalCount,
        long enrolledCount,
        long retirementCount,
        long disciplinaryCount
) {
}