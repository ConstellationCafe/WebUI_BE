package com.help.erpweb.domain.academy.controller;

import com.help.erpweb.domain.academy.dto.response.LessonRecordSummaryResponse;
import com.help.erpweb.domain.academy.service.LessonRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/academy")
public class LessonRecordController {

    private final LessonRecordService lessonRecordService;

    @GetMapping("/lesson-records")
    public List<LessonRecordSummaryResponse> getLessonRecords(
            @RequestParam(required = false)
            LocalDate date,

            @RequestParam(required = false)
            String time,

            @RequestParam(required = false)
            String subject,

            @RequestParam(required = false)
            String teacherId
    ) {
        return lessonRecordService.getLessonRecords(
                date,
                time,
                subject,
                teacherId
        );
    }
}