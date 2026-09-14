package com.help.erpweb.domain.academy.dto.response;

import com.help.erpweb.domain.academy.type.StudentRosterStatus;

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