package com.help.erpweb.domain.membership.controller;

import com.help.erpweb.domain.membership.dto.request.AdminPointTransactionRequest;
import com.help.erpweb.domain.membership.dto.response.AdminPointDetailResponse;
import com.help.erpweb.domain.membership.dto.response.AdminPointMemberPageResponse;
import com.help.erpweb.domain.membership.service.AdminPointService;
import com.help.global.common.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/repository/membership/admin/points")
@RequiredArgsConstructor
@Validated
@PreAuthorize("@authorization.isAdmin(authentication)")
public class AdminPointController {
    private final AdminPointService adminPointService;

    @GetMapping("/members")
    public ApiResponse<AdminPointMemberPageResponse> getMembers(
            @RequestParam(required = false) @Pattern(regexp = "[0-9]{1,20}") String discordId,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(adminPointService.getMembers(discordId, page, size));
    }

    @GetMapping("/members/{discordId}")
    public ApiResponse<AdminPointDetailResponse> getMember(
            @PathVariable @Pattern(regexp = "[0-9]{1,20}") String discordId,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(adminPointService.getMember(discordId, page, size));
    }

    @PostMapping("/members/{discordId}/transactions")
    public ApiResponse<AdminPointDetailResponse> transact(
            @PathVariable @Pattern(regexp = "[0-9]{1,20}") String discordId,
            @Valid @RequestBody AdminPointTransactionRequest request
    ) {
        return ApiResponse.success(adminPointService.transact(discordId, request));
    }
}
