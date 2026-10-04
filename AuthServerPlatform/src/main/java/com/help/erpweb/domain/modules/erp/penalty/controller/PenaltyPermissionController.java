package com.help.erpweb.domain.modules.erp.penalty.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.help.erpweb.domain.modules.erp.penalty.authorization.PenaltyAuthorization;
import com.help.erpweb.domain.modules.erp.penalty.dto.response.PenaltyPermissionResponse;
import com.help.global.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

/** 화면이 벌점 관리 메뉴를 보일지 정하는 권한 조회(ADR-0006). 로그인한 회원 누구나 호출한다. */
@RestController
@RequiredArgsConstructor
public class PenaltyPermissionController {
	private final PenaltyAuthorization authorization;

	@GetMapping(PenaltyController.BASE_PATH + "/me/permissions")
	public ApiResponse<PenaltyPermissionResponse> permissions(Authentication authentication) {
		return ApiResponse.success(new PenaltyPermissionResponse(authorization.isManager(authentication)));
	}
}
