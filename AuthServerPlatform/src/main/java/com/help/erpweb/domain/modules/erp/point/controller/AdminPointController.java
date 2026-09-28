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

/**
 * 관리자 포인트 API.
 * <p>
 * ADR-0002: 기준 경로는 {@value #BASE_PATH}다. {@value #DEPRECATED_BASE_PATH}는
 * BE/FE 배포 순서가 어긋나도 깨지지 않도록 남겨 둔 호환 경로이며, 새 경로를 쓰는
 * WebUI_FE가 운영에 배포된 뒤 제거한다. 호환 경로는 SecurityConfig의
 * /api/admin/** URL 규칙 밖에 있으므로 이 클래스의 {@code @PreAuthorize}가
 * 유일한 관리자 검사다. 제거 전까지 이 어노테이션을 지우지 않는다.
 */
@RestController
@RequestMapping({AdminPointController.BASE_PATH, AdminPointController.DEPRECATED_BASE_PATH})
@RequiredArgsConstructor
@Validated
@PreAuthorize("@authorization.isAdmin(authentication)")
public class AdminPointController {
    static final String BASE_PATH = "/api/admin/points";
    /** @deprecated ADR-0002. WebUI_FE 배포 후 제거한다. */
    @Deprecated
    static final String DEPRECATED_BASE_PATH = "/api/repository/membership/admin/points";

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
