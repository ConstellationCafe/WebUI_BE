package com.help.erpweb.domain.academy.controller;

import com.help.erpweb.domain.academy.service.AcademyService;
import com.help.global.common.response.ApiResponse;
import com.help.global.jwt.CustomUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/academies")
public class AcademyController {

    private final AcademyService academyService;

    @GetMapping
    public ApiResponse<?> getAcademies() {

        return ApiResponse.success(
                academyService.getAcademies()
        );
    }

    @GetMapping("/{academyId}/classes")
    public ApiResponse<?> getClasses(
            @PathVariable Integer academyId
    ) {

        return ApiResponse.success(
                academyService.getClasses(academyId)
        );
    }

    @GetMapping("/{academyId}/teachers")
    public ApiResponse<?> getTeachers(
            @PathVariable Integer academyId
    ) {

        return ApiResponse.success(
                academyService.getTeachers(academyId)
        );
    }

    @GetMapping("/me/sk")
    public ApiResponse<?> getMySk(
            CustomUser user
    ) {
        return ApiResponse.success(
                academyService.findUserSk(user)
        );
    }

    @GetMapping("/guild/{guildId}/config")
    public ApiResponse<?> getAcademyConfig(
            @PathVariable Long guildId
    ) {

        return ApiResponse.success(
                academyService.getAcademyConfig(guildId)
        );
    }
}