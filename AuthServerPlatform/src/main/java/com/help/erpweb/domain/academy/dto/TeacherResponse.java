package com.help.erpweb.domain.academy.dto;

public record TeacherResponse(
        Integer id,
        String name,
        Integer classId,
        Integer classNumber
) {
}