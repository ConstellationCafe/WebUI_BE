package com.help.erpweb.domain.academy.controller;

import com.help.erpweb.domain.academy.service.AcademyService;
import com.help.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/academy")
public class AcademyController {
    private final AcademyService academyService;

    @GetMapping("/me/permissions")
    public ApiResponse<?> getMyPermissions(
            Authentication authentication
    ) {
        log.info(
                "[GET] /api/academy/me/permissions user={}",
                authentication.getName()
        );
        return ApiResponse.success(
                academyService.getMyPermissions(authentication)
        );
    }

    @GetMapping
    public ApiResponse<?> getAcademies(
            Authentication authentication
    ) {
        log.info("[GET] /api/academy");
        return ApiResponse.success(
                academyService.getAcademies(authentication)
        );
    }

    @GetMapping("/{academyId}/classes")
    public ApiResponse<?> getClasses(
            @PathVariable Integer academyId
    ) {
        log.info(
                "[GET] /api/academy/{}/classes",
                academyId
        );
        return ApiResponse.success(
                academyService.getClasses(academyId)
        );
    }

    @GetMapping("/{academyId}/subjects")
    public ApiResponse<?> getSubjects(
            @PathVariable Integer academyId
    ) {
        log.info(
                "[GET] /api/academy/{}/subjects",
                academyId
        );
        return ApiResponse.success(
                academyService.getSubjects(academyId)
        );
    }

    // getTeachers -> TeacherController(/api/academy/teachers/{academyId}/classes/{classId})로 이관
    // getStudents -> StudentController(/api/academy/students/{academyId}/classes/{classId})로 이관
}
