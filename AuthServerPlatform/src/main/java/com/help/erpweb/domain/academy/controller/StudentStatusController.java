package com.help.erpweb.domain.academy.controller;

import com.help.erpweb.domain.academy.dto.request.StudentStatusQueryRequest;
import com.help.erpweb.domain.academy.service.StudentStatusService;
import com.help.erpweb.domain.academy.type.StudentRosterStatus;
import com.help.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/academy/student-status")
public class StudentStatusController {

    private final StudentStatusService studentStatusService;

    @GetMapping
    public ApiResponse<?> getStudentStatuses(
            @RequestParam(required = false)
            Integer academyId,

            @RequestParam(required = false)
            Integer classId,

            @RequestParam(required = false)
            String studentId,

            @RequestParam(required = false)
            StudentRosterStatus status,

            @RequestParam(
                    defaultValue = "1"
            )
            Integer page,

            @RequestParam(
                    defaultValue = "20"
            )
            Integer size
    ) {
        log.info(
                "[GET] /academy/student-status "
                        + "academyId={}, "
                        + "classId={}, "
                        + "studentId={}, "
                        + "status={}, "
                        + "page={}, "
                        + "size={}",
                academyId,
                classId,
                studentId,
                status,
                page,
                size
        );

        StudentStatusQueryRequest request =
                new StudentStatusQueryRequest(
                        academyId,
                        classId,
                        studentId,
                        status,
                        page,
                        size
                );

        return ApiResponse.success(
                studentStatusService
                        .getStudentStatuses(
                                request
                        )
        );
    }
}