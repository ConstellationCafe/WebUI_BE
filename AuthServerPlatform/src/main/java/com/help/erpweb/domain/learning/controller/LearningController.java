package com.help.erpweb.domain.learning.controller;

import com.help.global.common.response.ApiResponse;
import com.help.global.jwt.CustomUser;
import com.help.erpweb.domain.learning.dto.request.repository.LearningDto;
import com.help.erpweb.domain.learning.service.LearningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/repository/learning")
@Slf4j
@RequiredArgsConstructor
public class LearningController {
    private final LearningService learningService;

    @GetMapping("/list")
    public ApiResponse<?> getLearningList(@AuthenticationPrincipal CustomUser user) {
        return learningService.getLearningList(user);
    }

    @PostMapping("/save_all")
    public ApiResponse<?> saveAll(
            @AuthenticationPrincipal CustomUser user,
            @Valid @RequestBody final List<LearningDto> learningList
    ) {
        return learningService.saveAll(user, learningList);
    }

    @PostMapping("/delete_all")
    public ApiResponse<?> deleteAll(
            @AuthenticationPrincipal CustomUser user,
            @Valid @RequestBody final List<LearningDto> learningList
    ) {
        return learningService.deleteAll(user, learningList);
    }
}
