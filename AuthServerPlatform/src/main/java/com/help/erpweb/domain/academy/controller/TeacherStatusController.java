package com.help.erpweb.domain.academy.controller;

import com.help.erpweb.domain.academy.service.TeacherStatusService;
import com.help.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/academy/teacher-status")
public class TeacherStatusController {

    private final TeacherStatusService teacherStatusService;

    /**
     * 교사 상태 처리 화면의 선택 옵션 조회
     *
     * academyId == null
     *   -> 학원 목록 조회
     *
     * academyId != null
     *   -> 해당 학원의 분반 / 교사 조회
     *
     * classId가 전달되는 경우
     *   -> 해당 분반 기준 교사 필터링에 사용할 수 있음
     */
    @GetMapping("/options")
    public ApiResponse<?> getStatusOptions(
            @RequestParam(required = false) Integer academyId,
            @RequestParam(required = false) Integer classId
    ) {
        log.info(
                "[GET] /api/academy/teacher-status/options academyId={}, classId={}",
                academyId,
                classId
        );

        return ApiResponse.success(
                teacherStatusService.getStatusOptions(
                        academyId,
                        classId
                )
        );
    }

    /**
     * 교사 상태 이력 조회
     */
    @GetMapping
    public ApiResponse<?> getTeacherStatuses(
            @RequestParam(required = false) Integer academyId,
            @RequestParam(required = false) Integer classId,
            @RequestParam(required = false) String academyMemberId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info(
                "[GET] /api/academy/teacher-status " +
                        "academyId={}, classId={}, academyMemberId={}, status={}, page={}, size={}",
                academyId,
                classId,
                academyMemberId,
                status,
                page,
                size
        );

        return ApiResponse.success(
                teacherStatusService.getTeacherStatuses(
                        academyId,
                        classId,
                        academyMemberId,
                        status,
                        page,
                        size
                )
        );
    }
}