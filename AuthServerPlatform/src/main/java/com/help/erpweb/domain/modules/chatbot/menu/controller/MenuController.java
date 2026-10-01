package com.help.erpweb.domain.modules.chatbot.menu.controller;

import com.help.erpweb.domain.modules.chatbot.menu.dto.request.repository.MenuDto;
import com.help.erpweb.domain.modules.chatbot.menu.service.MenuService;
import com.help.global.common.response.ApiResponse;
import com.help.global.jwt.CustomUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/repository/menu")
@Slf4j
@RequiredArgsConstructor
@Validated
public class MenuController {
    private final MenuService menuService;

    @GetMapping("/list")
    public ApiResponse<?> getMenuList(
            @AuthenticationPrincipal CustomUser user,
            // Pagination
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            // Search
            @RequestParam(required = false) String searchColumn,
            @RequestParam(required = false) String searchValue,
            // Sort
            @RequestParam(required = false) String sortColumn,
            @RequestParam(defaultValue = "DESC") String sortDirection
    ) {
        return menuService.getMenuList(
                user, page, size,
                searchColumn, searchValue, sortColumn, sortDirection
        );
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
