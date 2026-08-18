package com.help.erpweb.domain.academy.dto.response;

public record StudentResponse(
        String id,
        String name,
        Integer academyId,
        Integer classId,
        Integer classNumber
) {
}