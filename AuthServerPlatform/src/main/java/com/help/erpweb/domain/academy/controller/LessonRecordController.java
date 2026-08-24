package com.help.erpweb.domain.academy.controller;

import com.help.erpweb.domain.academy.dto.request.LessonRecordCreateRequest;
import com.help.erpweb.domain.academy.dto.response.LessonRecordSummaryResponse;
import com.help.erpweb.domain.academy.service.LessonRecordService;
import com.help.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
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
            String teacherId,

            @RequestParam(required = false)
            Integer academyId,

            @RequestParam(required = false)
            Integer classId
    ) {
        log.info(
                "[GET] /api/academy/lesson-records " +
                        "academyId={}, classId={}, date={}, time={}, subject={}, teacherId={}",
                academyId,
                classId,
                date,
                time,
                subject,
                teacherId
        );

        return lessonRecordService.getLessonRecords(
                date,
                time,
                subject,
                teacherId,
                academyId,
                classId
        );
    }

    @PostMapping("/lesson-record")
    public ApiResponse<?> lessonRecord(
            @RequestBody LessonRecordCreateRequest request
    ) {
        log.info(
                "[POST] /api/academy/lesson-record " +
                        "academyId={}, className={}, subject={}, educationDate={}",
                request.academyId(),
                request.className(),
                request.subject(),
                request.educationDate()
        );

        lessonRecordService.createLessonRecord(request);

        return ApiResponse.success(null);
    }
}