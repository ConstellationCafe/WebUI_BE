package com.help.erpweb.domain.academy.dto.request;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record LessonRecordUpdateRequest(
        String subject,
        LocalDateTime educationDate,
        LocalTime startTime,
        LocalTime endTime,
        Integer educationDuration,
        String description
) {
}
