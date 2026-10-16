package com.help.erpweb.domain.modules.academy.dto.request;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;

public record LessonRecordCreateRequest(
        Integer academyId,
        String className,
        String subject,
        LocalDateTime educationDate,
        LocalTime startTime,
        LocalTime endTime,
        Integer educationDuration,
        // ADR-0008: 값은 모두 SK다. 이전 이름(*Id/*Ids)도 FE 전환 기간 동안 받는다.
        @JsonAlias("mainTeacherId") String mainTeacherSk,
        @JsonAlias("coTeacherIds") List<String> coTeacherSks,
        @JsonAlias("memberIds") List<String> memberSks,
        String description
) {
}
