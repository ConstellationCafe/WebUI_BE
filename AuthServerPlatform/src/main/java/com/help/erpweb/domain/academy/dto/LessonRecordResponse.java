package com.help.erpweb.domain.academy.dto;

import java.time.LocalDateTime;
import java.util.List;

public record LessonRecordResponse(
        Long id,
        Integer academyId,
        Integer classId,
        String subject,
        Integer mainTeacherId,
        List<Integer> coTeacherIds,
        List<Integer> memberIds,
        String description,
        LocalDateTime createdAt
) {
}