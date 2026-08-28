package com.help.erpweb.domain.academy.dto.response;

import java.time.LocalDate;

public record StatusItemResponse<T>(
        T academyMember,
        AcademyResponse academy,
        StatusClassResponse academyClass,
        String status,
        LocalDate statusChangedAt,
        String reason
) {
}