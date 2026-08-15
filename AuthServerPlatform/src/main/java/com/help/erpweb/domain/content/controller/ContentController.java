package com.help.erpweb.domain.content.controller;

import com.help.erpweb.domain.content.dto.request.repository.ContentDto;
import com.help.erpweb.domain.content.service.ContentService;
import com.help.global.common.response.ApiResponse;
import com.help.global.jwt.CustomUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/repository/content")
@Slf4j
@RequiredArgsConstructor
public class ContentController {
    private final ContentService ContentService;

    @GetMapping("/list")
    public ApiResponse<?> getContentList(@AuthenticationPrincipal CustomUser user) {
        return ContentService.getContentList(user);
    }

    @PostMapping("/save_all")
    public ApiResponse<?> saveAll(
            @AuthenticationPrincipal CustomUser user,
            @Valid @RequestBody final List<ContentDto> contentList
    ) {
        return ContentService.saveAll(user, contentList);
    }

    @PostMapping("/delete_all")
    public ApiResponse<?> deleteAll(
            @AuthenticationPrincipal CustomUser user,
            @Valid @RequestBody final List<ContentDto> contentList
    ) {
        return ContentService.deleteAll(user, contentList);
    }
}
