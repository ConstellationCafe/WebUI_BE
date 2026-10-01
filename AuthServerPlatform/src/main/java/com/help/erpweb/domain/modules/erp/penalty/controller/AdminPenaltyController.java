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

@RestController
@RequestMapping("/api/admin/penalties")
@PreAuthorize("@authorization.isAdmin(authentication)")
@Validated
@RequiredArgsConstructor
public class AdminPenaltyController {
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
