package com.help.backend.domain.music.controller;

import com.help.backend.domain.music.dto.request.repository.MusicDto;
import com.help.backend.domain.music.service.MusicService;
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
    public ApiResponse<?> getMusicList(@AuthenticationPrincipal CustomUser user) {
        return MusicService.getMusicList(user);
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
