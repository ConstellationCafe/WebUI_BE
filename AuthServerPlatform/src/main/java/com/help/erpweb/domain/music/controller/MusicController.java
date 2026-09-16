package com.help.erpweb.domain.music.controller;

import com.help.erpweb.domain.music.dto.request.repository.MusicDto;
import com.help.erpweb.domain.music.service.MusicService;
import com.help.global.common.response.ApiResponse;
import com.help.global.jwt.CustomUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/repository/music")
@Slf4j
@RequiredArgsConstructor
public class MusicController {
    private final MusicService MusicService;

    @GetMapping("/list")
    public ApiResponse<?> getMusicList(
            @AuthenticationPrincipal CustomUser user,
            // Pagination
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            // Search
            @RequestParam(required = false) String searchColumn,
            @RequestParam(required = false) String searchValue,
            // Sort
            @RequestParam(required = false) String sortColumn,
            @RequestParam(defaultValue = "DESC") String sortDirection
    ) {
        return MusicService.getMusicList(
                user, page, size,
                searchColumn, searchValue, sortColumn, sortDirection
        );
    }

    @PostMapping("/save_all")
    public ApiResponse<?> saveAll(
            @AuthenticationPrincipal CustomUser user,
            @Valid @RequestBody final List<MusicDto> musicList
    ) {
        return MusicService.saveAll(user, musicList);
    }

    @PostMapping("/delete_all")
    public ApiResponse<?> deleteAll(
            @AuthenticationPrincipal CustomUser user,
            @Valid @RequestBody final List<MusicDto> musicList
    ) {
        return MusicService.deleteAll(user, musicList);
    }
}
