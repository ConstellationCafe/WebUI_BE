package com.help.erpweb.domain.academy.dto.response;

public record TeacherResponse(
        String id,
        String name,
        Integer classId,
        Integer classNumber
) {
}