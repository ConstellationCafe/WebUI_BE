package com.help.erpweb.domain.academy.dto.response;

public record StudentResponse(
        String sk,
        String discordID,
        String name,
        Integer academyId,
        Integer classId,
        Integer classNumber
) {
}