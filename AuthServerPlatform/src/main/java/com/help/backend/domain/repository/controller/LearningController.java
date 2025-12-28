package com.help.backend.domain.repository.controller;

import com.help.global.common.response.ApiResponse;
import com.help.global.jwt.CustomUser;
import com.help.backend.domain.repository.dto.request.repository.LearningDto;
import com.help.backend.domain.repository.service.LearningService;
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
            @Valid @RequestBody final List<LearningDto> learningList
    ) {
        return learningService.saveAll(learningList);
    }
}
