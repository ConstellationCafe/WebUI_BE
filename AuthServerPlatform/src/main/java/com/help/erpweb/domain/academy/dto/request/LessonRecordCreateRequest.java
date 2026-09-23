package com.help.erpweb.domain.academy.dto.request;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record LessonRecordCreateRequest(
        Integer academyId,
        String className,
        String subject,
        LocalDateTime educationDate,
        LocalTime startTime,
        LocalTime endTime,
        Integer educationDuration,
        String mainTeacherId,
        List<String> coTeacherIds,
        List<String> memberIds,
        String description
) {
}
