package com.help.erpweb.domain.academy.controller;

import com.help.erpweb.domain.academy.service.AcademyService;
import com.help.erpweb.domain.academy.service.StudentStatusService;
import com.help.global.common.response.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 2026-09-26: StudentStatusController에서 이름을 바꾸고(학생 상태 이력 조회만
 * 다루던 컨트롤러 -> "학생" 리소스 전반을 다루는 컨트롤러), AcademyController의
 * getStudents를 이 컨트롤러로 옮겼다. 그에 맞춰 URL도 리소스 기준으로
 * 재정리했다 — API 명세가 일부 바뀐다(FE/Notion 갱신 필요).
 *
 *   (기존) GET /api/academy/student-status/options
 *        -> GET /api/academy/students/options
 *   (기존) GET /api/academy/student-status
 *        -> GET /api/academy/students
 *   (기존) GET /api/academy/{academyId}/classes/{classId}/students
 *        -> GET /api/academy/students/{academyId}/classes/{classId}
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/academy/students")
@Validated
public class StudentController {
    private final StudentStatusService studentStatusService;
    private final AcademyService academyService;

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
                "[GET] /api/academy/students/options academyId={}, classId={}",
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
    /**
     * 2026-09-26: 교사 이상만 접근 가능하도록 인가 추가 (FE에서는 학생 관리/조회
     * 메뉴가 isTeacherOrAbove() 기준이라 학원장뿐 아니라 담당 교사도 접근 가능
     * — getTeacherStatuses와 달리 isOwner 전용이 아니다). academyId 없이(전체
     * 조회) 호출되면 "어느 Academy에서든 교사 이상인지"만 확인한다.
     */
    @PreAuthorize(
            "#academyId == null "
                    + "? @academyAuth.isTeacherOrAbove(authentication) "
                    + ": @academyAuth.isTeacherOrAboveOfAcademy(authentication, #academyId)"
    )
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
                "[GET] /api/academy/students " +
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

    /**
     * 특정 분반의 학생 목록 조회 (AcademyController.getStudents에서 이관됨)
     */
    @PreAuthorize("@academyAuth.canManageClass(authentication, #academyId, #classId)")
    @GetMapping("/{academyId}/classes/{classId}")
    public ApiResponse<?> getStudents(
            @PathVariable Integer academyId,
            @PathVariable Integer classId
    ) {
        log.info(
                "[GET] /api/academy/students/{}/classes/{}",
                academyId, classId
        );
        return ApiResponse.success(
                academyService.getStudents(
                        academyId,
                        classId
                )
        );
    }
}
