package com.help.erpweb.domain.membership.controller;

import com.help.erpweb.domain.membership.service.PointService;
import com.help.global.common.response.ApiResponse;
import com.help.global.jwt.CustomUser;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 2026-09-26: MembershipController에서 개명. 실제로는 본인 포인트 로그 조회
 * 하나뿐인 엔드포인트라 "Membership"이라는 이름이 내용과 맞지 않았다.
 * AdminPointController와 짝을 이루는 이름으로 바꿨다 — 인가 수준만 다를 뿐
 * 같은 PointService를 공유한다.
 */
@RestController
@RequestMapping("/api/repository/membership")
@Slf4j
@RequiredArgsConstructor
@Validated
public class PointController {
    private final PointService pointService;

    @GetMapping("/point_log")
    public ApiResponse<?> getPointLog(
            @AuthenticationPrincipal CustomUser user,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return pointService.getPointLog(
                user,
                page,
                size
        );
    }
}
