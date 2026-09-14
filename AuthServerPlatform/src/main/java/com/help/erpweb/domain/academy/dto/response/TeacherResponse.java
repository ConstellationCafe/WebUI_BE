package com.help.erpweb.domain.academy.dto.response;

public record TeacherResponse(
        String sk,
        String discordID,
        String name,
        Integer classId,
        Integer classNumber
) {
}