package com.help.erpweb.domain.modules.competition.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.help.erpweb.domain.modules.competition.dto.request.CompetitionCreateRequest;
import com.help.erpweb.domain.modules.competition.dto.request.CompetitionNoticeRequest;
import com.help.erpweb.domain.modules.competition.dto.response.CompetitionBoardResponse;
import com.help.erpweb.domain.modules.competition.dto.response.CompetitionPostResponse;
import com.help.erpweb.domain.modules.competition.dto.response.CompetitionPreviewResponse;
import com.help.erpweb.domain.modules.competition.service.CompetitionService;
import com.help.global.common.response.ApiResponse;
import com.help.global.jwt.CustomUser;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 관리자 대회 공지(ADR-0002 경로 규칙: /api/admin/**). 대상 채팅방은 로그인한 채팅방(botId)으로 고정한다.
 */
@RestController
@RequestMapping("/api/admin/competitions")
@PreAuthorize("@authorization.isAdmin(authentication)")
@Validated
@RequiredArgsConstructor
public class AdminCompetitionController {
	private final CompetitionService service;

	@GetMapping("/boards")
	public ApiResponse<List<CompetitionBoardResponse>> boards() {
		return ApiResponse.success(service.boards());
	}

	/** 게시하지 않고 검증과 평문 조립만 한다. 화면 미리보기가 실제 게시글과 같게 하려고 둔다. */
	@PostMapping("/preview")
	public ApiResponse<CompetitionPreviewResponse> preview(@Valid @RequestBody CompetitionNoticeRequest request) {
		return ApiResponse.success(service.preview(request.toNotice()));
	}

	@PostMapping
	public ApiResponse<CompetitionPostResponse> post(
		@Valid @RequestBody CompetitionCreateRequest request,
		@AuthenticationPrincipal CustomUser admin
	) {
		return ApiResponse.success(service.post(
			request.requestId(), request.boardKey(), request.notice().toNotice(), admin.getUsername()));
	}
}
