package com.help.erpweb.domain.modules.competition.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.help.erpweb.domain.modules.competition.dto.request.CompetitionWinnerGrantRequest;
import com.help.erpweb.domain.modules.competition.dto.response.CompetitionWinnerPageResponse;
import com.help.erpweb.domain.modules.competition.dto.response.CompetitionWinnerResponse;
import com.help.erpweb.domain.modules.competition.service.CompetitionWinnerService;
import com.help.global.common.response.ApiResponse;
import com.help.global.jwt.CustomUser;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

/** 대회 우승 칭호. 대회 매니저(또는 서버장)가 현재 채팅방의 재적 회원에게 부여한다. */
@RestController
@RequestMapping("/api/competitions/winners")
@PreAuthorize("@competitionAuth.isManager(authentication)")
@Validated
@RequiredArgsConstructor
public class CompetitionWinnerController {
	private final CompetitionWinnerService service;

	@PostMapping
	public ApiResponse<CompetitionWinnerResponse> grant(
		@Valid @RequestBody CompetitionWinnerGrantRequest request,
		@AuthenticationPrincipal CustomUser manager
	) {
		return ApiResponse.success(service.grant(
			request.competitionName(),
			request.version(),
			request.winnerDiscordId(),
			request.acquisition(),
			manager.getUsername()));
	}

	@GetMapping
	public ApiResponse<CompetitionWinnerPageResponse> history(
		@RequestParam(defaultValue = "1") @Min(1) int page,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
	) {
		return ApiResponse.success(service.history(page, size));
	}
}
