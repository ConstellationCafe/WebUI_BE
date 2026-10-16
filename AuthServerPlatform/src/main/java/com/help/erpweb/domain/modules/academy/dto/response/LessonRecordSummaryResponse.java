package com.help.erpweb.domain.modules.academy.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LessonRecordSummaryResponse(
        Long id,
        String academyName,
        String className,
        String subject,
        LocalDate educationDate,
        LocalTime startTime,
        LocalTime endTime,
        Integer educationDuration,
        String mainTeacherDisplayName,
        String description,
        Integer memberCount,
        boolean canModify
) {
    /** @deprecated ADR-0008 호환 필드. FE가 mainTeacherDisplayName으(으)로 전환한 뒤 제거한다. */
    @Deprecated
    @JsonProperty("mainTeacherName")
    public String legacyMainTeacherName() {
        return mainTeacherDisplayName;
    }
}
