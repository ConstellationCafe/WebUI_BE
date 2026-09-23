package com.help.erpweb.domain.membership.controller;

import com.help.erpweb.domain.membership.dto.request.repository.PointAdjustmentRequest;
import com.help.erpweb.domain.membership.service.MembershipService;
import com.help.global.common.response.ApiResponse;
import com.help.global.jwt.CustomUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/repository/membership")
@Slf4j
@RequiredArgsConstructor
@Validated
public class MembershipController {
    private final MembershipService membershipService;

    @GetMapping("/point_log")
    public ApiResponse<?> getPointLog(
            @AuthenticationPrincipal CustomUser user,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return membershipService.getPointLog(user, page, size);
    }

    @GetMapping("/admin/points/members")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<?> getActivePointMembers(
            @RequestParam(required = false) @Size(max = 64) String discordId,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return membershipService.getActivePointMembers(discordId, page, size);
    }

    @GetMapping("/admin/points/members/{discordId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<?> getPointMemberDetail(
            @PathVariable @Size(min = 1, max = 64) String discordId,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return membershipService.getPointMemberDetail(discordId, page, size);
    }

    @PostMapping("/admin/points/members/{discordId}/transactions")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<?> adjustPoint(
            @PathVariable @Size(min = 1, max = 64) String discordId,
            @Valid @RequestBody PointAdjustmentRequest request
    ) {
        return membershipService.adjustPoint(discordId, request);
    }
}
