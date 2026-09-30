package com.help.erpweb.domain.modules.competition.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.help.erpweb.domain.modules.competition.authorization.CompetitionAuthorization;
import com.help.erpweb.domain.modules.competition.dto.request.CompetitionCreateRequest;
import com.help.erpweb.domain.modules.competition.dto.request.CompetitionNoticeRequest;
import com.help.erpweb.domain.modules.competition.dto.response.CompetitionBoardResponse;
import com.help.erpweb.domain.modules.competition.dto.response.CompetitionPermissionResponse;
import com.help.erpweb.domain.modules.competition.dto.response.CompetitionPostResponse;
import com.help.erpweb.domain.modules.competition.dto.response.CompetitionPreviewResponse;
import com.help.erpweb.domain.modules.competition.service.CompetitionService;
import com.help.global.common.response.ApiResponse;
import com.help.global.jwt.CustomUser;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 대회 기능. 서버장 전용이 아니라 대회 매니저도 쓰므로 {@code /api/admin/**}(ROLE_ADMIN URL 규칙) 밖에 두고,
 * 아카데미처럼 메서드마다 {@code @competitionAuth}로 권한을 확인한다. 대상 채팅방은 로그인한 채팅방(botId)이다.
 */
@RestController
@RequestMapping("/api/competitions")
@Validated
@RequiredArgsConstructor
public class CompetitionController {
	private final CompetitionService service;
	private final CompetitionAuthorization authorization;

	/** 로그인한 회원 누구나 호출한다. 화면이 대회 메뉴를 보일지 정한다. */
	@GetMapping("/me/permissions")
	public ApiResponse<CompetitionPermissionResponse> permissions(Authentication authentication) {
		return ApiResponse.success(new CompetitionPermissionResponse(authorization.isManager(authentication)));
	}

	@PreAuthorize("@competitionAuth.isManager(authentication)")
	@GetMapping("/boards")
	public ApiResponse<List<CompetitionBoardResponse>> boards() {
		return ApiResponse.success(service.boards());
	}

	/** 게시하지 않고 검증과 평문 조립만 한다. 화면 미리보기가 실제 게시글과 같게 하려고 둔다. */
	@PreAuthorize("@competitionAuth.isManager(authentication)")
	@PostMapping("/notices/preview")
	public ApiResponse<CompetitionPreviewResponse> preview(@Valid @RequestBody CompetitionNoticeRequest request) {
		return ApiResponse.success(service.preview(request.toNotice()));
	}

	@PreAuthorize("@competitionAuth.isManager(authentication)")
	@PostMapping("/notices")
	public ApiResponse<CompetitionPostResponse> post(
		@Valid @RequestBody CompetitionCreateRequest request,
		@AuthenticationPrincipal CustomUser manager
	) {
		return ApiResponse.success(service.post(
			request.requestId(), request.boardKey(), request.notice().toNotice(), manager.getUsername()));
	}
}
