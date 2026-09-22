package com.help.erpweb.domain.academy.controller;

import com.help.erpweb.domain.academy.service.StudentStatusService;
import com.help.global.common.response.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/academy/student-status")
@Validated
public class StudentStatusController {
    private final StudentStatusService studentStatusService;
    /**
     * 학생 상태 처리 화면의 선택 옵션 조회
     *
     * academyId == null
     *   -> 학원 목록 조회
     *
     * academyId != null, classId == null
     *   -> 분반 / 교과목 조회
     *
     * academyId != null, classId != null
     *   -> 해당 분반 학생 조회
     */
    @GetMapping("/options")
    public ApiResponse<?> getStatusOptions(
            @RequestParam(required = false) Integer academyId,
            @RequestParam(required = false) Integer classId
    ) {
        log.info(
                "[GET] /api/academy/student-status/options academyId={}, classId={}",
                academyId,
                classId
        );

        return ApiResponse.success(
                studentStatusService.getStatusOptions(
                        academyId,
                        classId
                )
        );
    }

    /**
     * 학생 상태 이력 조회
     *
     * 모든 조건은 선택 사항이다.
     *
     * academyMemberId는 기존 studentId 대신
     * 학생/교사 상태 조회 API에서 공통으로 사용하는 이름이다.
     */
    @GetMapping
    public ApiResponse<?> getStudentStatuses(
            @RequestParam(required = false) Integer academyId,
            @RequestParam(required = false) Integer classId,
            @RequestParam(required = false) String academyMemberId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        log.info(
                "[GET] /api/academy/student-status " +
                        "academyId={}, classId={}, academyMemberId={}, status={}, page={}, size={}",
                academyId,
                classId,
                academyMemberId,
                status,
                page,
                size
        );

        return ApiResponse.success(
                studentStatusService.getStudentStatuses(
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
