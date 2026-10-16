package com.help.erpweb.domain.modules.academy.dto.response;

import com.help.erpweb.domain.modules.academy.type.StudentRosterStatus;

import java.time.LocalDate;

public record StudentStatusItemResponse(
        StudentInfoResponse student,
        AcademyResponse academy,
        ClassResponse academyClass,
        StudentRosterStatus status,
        LocalDate statusChangedAt,
        String reason
) {
}
