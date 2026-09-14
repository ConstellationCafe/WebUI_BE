package com.help.erpweb.domain.academy.dto.response;

import java.time.LocalDate;

public record LessonRecordSummaryResponse(
        Long id,
        String academyName,
        String className,
        String subject,
        LocalDate educationDate,
        Integer educationDuration,
        String mainTeacherName,
        String description,
        Integer memberCount
) {
}