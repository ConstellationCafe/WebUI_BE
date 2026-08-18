package com.help.erpweb.domain.academy.dto.request;

import java.time.LocalDateTime;
import java.util.List;

public record LessonRecordCreateRequest(
        Integer academyId,
        Integer classId,
        String subject,
        LocalDateTime educationDate,
        Integer educationDuration,
        String mainTeacherId,
        List<String> coTeacherIds,
        List<String> memberIds,
        String description
) {
}