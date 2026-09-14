package com.help.erpweb.domain.membership.controller;

import com.help.erpweb.domain.membership.service.MembershipService;
import com.help.global.common.response.ApiResponse;
import com.help.global.jwt.CustomUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/repository/membership")
@Slf4j
@RequiredArgsConstructor
public class MembershipController {
    private final MembershipService membershipService;

    @GetMapping("/point_log")
    public ApiResponse<?> getPointLog(@AuthenticationPrincipal CustomUser user) {
        return membershipService.getPointLog(user);
    }
}
