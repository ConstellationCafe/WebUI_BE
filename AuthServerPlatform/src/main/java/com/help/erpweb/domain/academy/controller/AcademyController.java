package com.help.erpweb.domain.academy.controller;

import com.help.erpweb.domain.academy.dto.request.LessonRecordCreateRequest;
import com.help.erpweb.domain.academy.service.AcademyService;
import com.help.global.common.response.ApiResponse;
import com.help.global.jwt.CustomUser;
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
        log.info("[GET] /academy");
        return ApiResponse.success(
                academyService.getAcademies()
        );
    }

    @GetMapping("/{academyId}/classes")
    public ApiResponse<?> getClasses(
            @PathVariable Integer academyId
    ) {
        log.info("[GET] /academy/classes");
        return ApiResponse.success(
                academyService.getClasses(academyId)
        );
    }

    @GetMapping("/{academyId}/subjects")
    public ApiResponse<?> getSubjects(
            @PathVariable Integer academyId
    ) {
        log.info("[GET] /academy/subjects");
        return ApiResponse.success(
                academyService.getSubjects(academyId)
        );
    }

    @GetMapping("/{academyId}/teachers")
    public ApiResponse<?> getTeachers(
            @PathVariable Integer academyId
    ) {
        log.info("[GET] /academy/{academyId}/teachers");
        return ApiResponse.success(
                academyService.getTeachers(academyId)
        );
    }

    @GetMapping("/{academyId}/classes/{classId}/students")
    public ApiResponse<?> getStudents(
            @PathVariable Integer academyId,
            @PathVariable Integer classId
    ) {
        log.info("[GET] /academy/{academyId}/classes/{classId}/students");
        return ApiResponse.success(
                academyService.getStudents(academyId, classId)
        );
    }

    @PostMapping("/lesson-record")
    public ApiResponse<?> lessonRecord(
            @RequestBody LessonRecordCreateRequest request
    ) {
        log.info("[POST] /academy/record");
        academyService.createLessonRecord(request);
        return ApiResponse.success(null);
    }

//    @GetMapping("/me/sk")
//    public ApiResponse<?> getMySk(
//            CustomUser user
//    ) {
//        log.info("[GET] /academy/me/sk");
//        return ApiResponse.success(
//                academyService.findUserSk(user)
//        );
//    }
//
//    @GetMapping("/guild/{guildId}/config")
//    public ApiResponse<?> getAcademyConfig(
//            @PathVariable String guildId
//    ) {
//        log.info("[GET] /academy/guild/{guildId}/config");
//        return ApiResponse.success(
//                academyService.getAcademyConfig(guildId)
//        );
//    }
}