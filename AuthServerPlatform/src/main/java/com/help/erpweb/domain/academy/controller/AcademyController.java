package com.help.erpweb.domain.academy.controller;

import com.help.erpweb.domain.academy.service.AcademyService;
import com.help.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/academy")
public class AcademyController {

    private final AcademyService academyService;

    @GetMapping
    public ApiResponse<?> getAcademies() {
        log.info("[GET] /api/academy");

        return ApiResponse.success(
                academyService.getAcademies()
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

    @GetMapping("/{academyId}/teachers")
    public ApiResponse<?> getTeachers(
            @PathVariable Integer academyId
    ) {
        log.info(
                "[GET] /api/academy/{}/teachers",
                academyId
        );

        return ApiResponse.success(
                academyService.getTeachers(academyId)
        );
    }

    @GetMapping("/{academyId}/classes/{classId}/students")
    public ApiResponse<?> getStudents(
            @PathVariable Integer academyId,
            @PathVariable Integer classId
    ) {
        log.info(
                "[GET] /api/academy/{}/classes/{}/students",
                academyId,
                classId
        );

        return ApiResponse.success(
                academyService.getStudents(
                        academyId,
                        classId
                )
        );
    }
}