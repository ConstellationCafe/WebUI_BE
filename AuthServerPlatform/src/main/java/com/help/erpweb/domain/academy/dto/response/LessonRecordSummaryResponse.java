package com.help.erpweb.domain.academy.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;

public record LessonRecordSummaryResponse(
        Long id,
        String academyName,
        String className,
        String subject,
        LocalDate educationDate,
        LocalTime startTime,
        LocalTime endTime,
        Integer educationDuration,
        String mainTeacherName,
        String description,
        Integer memberCount,
        boolean canModify
) {
}
