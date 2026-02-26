package com.help.backend.domain.menu.controller;

import com.help.backend.domain.menu.dto.request.repository.MenuDto;
import com.help.backend.domain.menu.service.MenuService;
import com.help.global.common.response.ApiResponse;
import com.help.global.jwt.CustomUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/repository/menu")
@Slf4j
@RequiredArgsConstructor
public class MenuController {
    private final MenuService menuService;

    @GetMapping("/list")
    public ApiResponse<?> getMenuList(@AuthenticationPrincipal CustomUser user) {
        return menuService.getMenuList(user);
    }

    @PostMapping("/save_all")
    public ApiResponse<?> saveAll(
            @AuthenticationPrincipal CustomUser user,
            @Valid @RequestBody final List<MenuDto> menuList
    ) {
        return menuService.saveAll(user, menuList);
    }

    @PostMapping("/delete_all")
    public ApiResponse<?> deleteAll(
            @AuthenticationPrincipal CustomUser user,
            @Valid @RequestBody final List<MenuDto> menuList
    ) {
        return menuService.deleteAll(user, menuList);
    }
}
