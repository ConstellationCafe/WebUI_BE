package com.help.erpweb.domain.modules.erp.point.controller;

import com.help.erpweb.domain.modules.erp.point.dto.request.AdminPointLogUpdateRequest;
import com.help.erpweb.domain.modules.erp.point.dto.request.AdminPointTransactionRequest;
import com.help.erpweb.domain.modules.erp.point.dto.response.AdminPointDetailResponse;
import com.help.erpweb.domain.modules.erp.point.dto.response.AdminPointMemberPageResponse;
import com.help.erpweb.domain.modules.erp.point.service.PointService;
import com.help.global.common.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
    private final PointService pointService;

    @GetMapping("/members")
    public ApiResponse<AdminPointMemberPageResponse> getMembers(
            @RequestParam(required = false) @Pattern(regexp = "[0-9]{1,20}") String discordId,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(pointService.getMembers(discordId, page, size));
    }

    @GetMapping("/members/{discordId}")
    public ApiResponse<AdminPointDetailResponse> getMember(
            @PathVariable @Pattern(regexp = "[0-9]{1,20}") String discordId,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(pointService.getMember(discordId, page, size));
    }

    @PostMapping("/members/{discordId}/transactions")
    public ApiResponse<AdminPointDetailResponse> transact(
            @PathVariable @Pattern(regexp = "[0-9]{1,20}") String discordId,
            @Valid @RequestBody AdminPointTransactionRequest request
    ) {
        return ApiResponse.success(pointService.transact(discordId, request));
    }

    @PatchMapping("/members/{discordId}/logs/{originalAmount}")
    public ApiResponse<AdminPointDetailResponse> updateLog(
            @PathVariable @Pattern(regexp = "[0-9]{1,20}") String discordId,
            @PathVariable int originalAmount,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime at,
            @Valid @RequestBody AdminPointLogUpdateRequest request
    ) {
        return ApiResponse.success(pointService.updateLog(
                discordId,
                originalAmount,
                at,
                request.amount(),
                request.description()
        ));
    }

    @DeleteMapping("/members/{discordId}/logs/{originalAmount}")
    public ApiResponse<AdminPointDetailResponse> deleteLog(
            @PathVariable @Pattern(regexp = "[0-9]{1,20}") String discordId,
            @PathVariable int originalAmount,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime at
    ) {
        return ApiResponse.success(pointService.deleteLog(discordId, originalAmount, at));
    }
}
