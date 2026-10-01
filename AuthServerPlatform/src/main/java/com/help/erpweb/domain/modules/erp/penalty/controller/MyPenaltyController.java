package com.help.erpweb.domain.modules.erp.penalty.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.help.erpweb.domain.modules.erp.penalty.dto.response.PenaltyMemberDetailResponse;
import com.help.erpweb.domain.modules.erp.penalty.service.PenaltyService;
import com.help.global.common.response.ApiResponse;
import com.help.global.jwt.CustomUser;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/me/penalties")
@Validated
@RequiredArgsConstructor
public class MyPenaltyController {
	private final PenaltyService service;

	@GetMapping
	public ApiResponse<PenaltyMemberDetailResponse> myPenalties(
			@AuthenticationPrincipal CustomUser user,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return ApiResponse.success(service.member(user.getUsername(), page, size));
	}
}
