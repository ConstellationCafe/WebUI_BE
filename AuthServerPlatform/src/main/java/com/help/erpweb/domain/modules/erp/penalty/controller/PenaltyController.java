package com.help.erpweb.domain.modules.erp.penalty.controller;

import java.time.Instant;
import java.time.format.DateTimeParseException;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.help.erpweb.domain.modules.erp.penalty.dto.request.PenaltyCancelRequest;
import com.help.erpweb.domain.modules.erp.penalty.dto.request.PenaltyCreateRequest;
import com.help.erpweb.domain.modules.erp.penalty.dto.request.PenaltySort;
import com.help.erpweb.domain.modules.erp.penalty.dto.response.PenaltyHistoryItemResponse;
import com.help.erpweb.domain.modules.erp.penalty.dto.response.PenaltyMemberDetailResponse;
import com.help.erpweb.domain.modules.erp.penalty.dto.response.PenaltyMemberRankResponse;
import com.help.erpweb.domain.modules.erp.penalty.dto.response.PenaltyPageResponse;
import com.help.erpweb.domain.modules.erp.penalty.service.PenaltyCreateCommand;
import com.help.erpweb.domain.modules.erp.penalty.service.PenaltyService;
import com.help.global.common.exception.CustomException;
import com.help.global.common.exception.ErrorCode;
import com.help.global.common.response.ApiResponse;
import com.help.global.jwt.CustomUser;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

/**
 * 벌점 관리 API(ADR-0006). 서버장 전용이 아니라 운영 매니저·운영 본부원도 쓰므로
 * {@value #BASE_PATH}에 두고 {@code @penaltyAuth}로 권한을 확인한다.
 *
 * <p>{@value #DEPRECATED_BASE_PATH}는 구버전 FE를 위한 호환 경로다. {@code /api/admin/**} URL 규칙(ADR-0002)으로
 * 계속 서버장만 통과하며, 신규 FE 배포 후 호출이 없음을 확인하면 제거한다.
 */
@RestController
@RequestMapping({PenaltyController.BASE_PATH, PenaltyController.DEPRECATED_BASE_PATH})
@PreAuthorize("@penaltyAuth.isManager(authentication)")
@Validated
@RequiredArgsConstructor
public class PenaltyController {
	static final String BASE_PATH = "/api/penalties";
	static final String DEPRECATED_BASE_PATH = "/api/admin/penalties";

	private final PenaltyService service;

	@PostMapping
	public ApiResponse<PenaltyMemberDetailResponse> create(
			@Valid @RequestBody PenaltyCreateRequest request,
			@AuthenticationPrincipal CustomUser issuer) {
		return ApiResponse.success(service.create(new PenaltyCreateCommand(
				request.requestId(), request.targetDiscordId(), request.channelId(),
				request.channelName(), request.reason(), request.score(), parseUtc(request.occurredAt()),
				issuer.getUsername())));
	}

	@GetMapping
	public ApiResponse<PenaltyPageResponse<PenaltyHistoryItemResponse>> history(
			@RequestParam(required = false) @Pattern(regexp = "[0-9]{1,20}") String channelId,
			@RequestParam(required = false) @Pattern(regexp = "[0-9]{1,20}") String discordId,
			@RequestParam(defaultValue = "OCCURRED_AT_DESC") PenaltySort sort,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return ApiResponse.success(service.history(channelId, discordId, sort, page, size));
	}

	@GetMapping("/members")
	public ApiResponse<PenaltyPageResponse<PenaltyMemberRankResponse>> ranking(
			@RequestParam(required = false) @Pattern(regexp = "[0-9]{1,20}") String discordId,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return ApiResponse.success(service.ranking(discordId, page, size));
	}

	@GetMapping("/members/{discordId}")
	public ApiResponse<PenaltyMemberDetailResponse> member(
			@PathVariable @Pattern(regexp = "[0-9]{1,20}") String discordId,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return ApiResponse.success(service.member(discordId, page, size));
	}

	@PatchMapping("/{penaltyId}/cancel")
	public ApiResponse<PenaltyMemberDetailResponse> cancel(
			@PathVariable @Positive long penaltyId,
			@Valid @RequestBody PenaltyCancelRequest request,
			@AuthenticationPrincipal CustomUser issuer) {
		return ApiResponse.success(service.cancel(penaltyId, issuer.getUsername(), request.reason()));
	}

	private Instant parseUtc(String occurredAt) {
		if (occurredAt == null) {
			return null;
		}
		try {
			return Instant.parse(occurredAt);
		} catch (DateTimeParseException ex) {
			throw new CustomException(ErrorCode.PENALTY_INVALID_OCCURRED_AT);
		}
	}
}
