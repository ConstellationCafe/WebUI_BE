package com.help.erpweb.domain.academy.controller;

import com.help.erpweb.domain.academy.service.AcademyService;
import com.help.erpweb.domain.academy.service.TeacherStatusService;
import com.help.global.common.response.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 2026-09-26: TeacherStatusController에서 이름을 바꾸고(교사 상태 이력 조회만
 * 다루던 컨트롤러 -> "교사" 리소스 전반을 다루는 컨트롤러), AcademyController의
 * getTeachers를 이 컨트롤러로 옮겼다. StudentController와 대칭되는 구조로
 * 맞췄다 — API 명세가 일부 바뀐다(FE/Notion 갱신 필요).
 *
 *   (기존) GET /api/academy/teacher-status/options
 *        -> GET /api/academy/teachers/options
 *   (기존) GET /api/academy/teacher-status
 *        -> GET /api/academy/teachers
 *   (기존) GET /api/academy/{academyId}/classes/{classId}/teachers
 *        -> GET /api/academy/teachers/{academyId}/classes/{classId}
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/academy/teachers")
@Validated
public class TeacherController {
    private final TeacherStatusService teacherStatusService;
    private final AcademyService academyService;

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
                "[GET] /api/academy/teachers/options academyId={}, classId={}",
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
     *
     * 2026-09-26: 학원장 이상만 접근 가능하도록 인가 추가 (FE academy_category.dart가
     * "교사 관리/조회" 메뉴 자체를 isOwner() 기준으로 숨기고 있었는데, API에는
     * 대응하는 서버 측 인가가 없었다). academyId 없이(전체 조회) 호출되면 "어느
     * Academy에서든 학원장인지"만 확인한다.
     */
    @PreAuthorize(
            "#academyId == null "
                    + "? @academyAuth.isOwnerOfAnyAcademy(authentication) "
                    + ": @academyAuth.isOwner(authentication, #academyId)"
    )
    @GetMapping
    public ApiResponse<?> getTeacherStatuses(
            @RequestParam(required = false) Integer academyId,
            @RequestParam(required = false) Integer classId,
            @RequestParam(required = false) String academyMemberId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        log.info(
                "[GET] /api/academy/teachers " +
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

    /**
     * 특정 분반의 교사 목록 조회 (AcademyController.getTeachers에서 이관됨)
     */
    @PreAuthorize("@academyAuth.isMember(authentication, #academyId)")
    @GetMapping("/{academyId}/classes/{classId}")
    public ApiResponse<?> getTeachers(
            @PathVariable Integer academyId,
            @PathVariable Integer classId
    ) {
        log.info(
                "[GET] /api/academy/teachers/{}/classes/{}",
                academyId, classId
        );
        return ApiResponse.success(
                academyService.getTeachers(academyId, classId)
        );
    }
}
