package com.help.erpweb.domain.academy.dto.request;

import java.util.List;

public record LessonRecordRequest(
        Integer classId,
        String subject,
        Integer mainTeacherId,
        List<Integer> coTeacherIds,
        List<Integer> memberIds,
        String description
) {
}