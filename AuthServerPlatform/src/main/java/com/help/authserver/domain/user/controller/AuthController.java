package com.help.authserver.domain.user.controller;

import com.help.authserver.domain.user.dto.request.GuildSelectRequestDto;
import com.help.authserver.domain.user.service.AuthSessionService;
import com.help.global.jwt.CustomUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.help.global.common.response.ApiResponse;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 일반 로그인 세션 관리(채팅방 선택, 토큰 갱신, 로그인 확인, 로그아웃) 컨트롤러.
 * Discord OAuth2 로그인 및 Discord 부가 정보 조회는 {@link DiscordAuthController}가 담당한다.
 */
@RestController
@RequestMapping("/auth")
@Slf4j
@RequiredArgsConstructor
public class AuthController {
	private final AuthSessionService authSessionService;

	// ADR-0001: discordId 인증 이후, 채팅방(guildId)을 선택해야 로그인이 완료된다.
	@PostMapping("/guild/select")
	public ApiResponse<?> selectGuild(
		@AuthenticationPrincipal CustomUser user,
		@Valid @RequestBody GuildSelectRequestDto request,
		HttpServletResponse response
	) {
		log.info("[POST] /auth/guild/select");
		return authSessionService.selectRoom(user, request.guildId(), response);
	}

	// AccessToken 갱신 요청
	@PostMapping("/refresh")
	public ApiResponse<?> refresh(
		final HttpServletRequest request,
		final HttpServletResponse response
	) {
		log.info("[POST] /auth/refresh");
		return authSessionService.refresh(request, response);
	}

	@GetMapping("/check")
	public ApiResponse<?> loginCheck(final HttpServletRequest request) {
		log.info("[GET] /auth/check");
		return authSessionService.checkLogin(request);
	}

	@PostMapping("/logout")
	public ApiResponse<?> logout(
			@AuthenticationPrincipal CustomUser user,
			final HttpServletResponse response
	) {
		log.info("[GET] /auth/logout");
		authSessionService.logout(user, response);
		return ApiResponse.success(null);
	}

//	@PreAuthorize("isAuthenticated()")
//	@PostMapping("/password-reset-request")
//	public ApiResponse<?> requestPasswordReset(@Valid @RequestBody final PasswordResetRequestDto resetRequestDto,
//		final HttpServletRequest request) {
//		final String email = resetRequestDto.email();
//
//		final String token = tokenService.createToken();
//		tokenService.saveResetEmailToRedis(token, email);
//		return ApiResponse.success(null);
//	}

//	@PreAuthorize("isAuthenticated()")
//	@PostMapping("/password-reset")
//	public ApiResponse<?> confirmPasswordReset(
//		@AuthenticationPrincipal final CustomUser userDetail,
//		@Valid @RequestBody final PasswordResetConfirmDto request
//	) {
//		final String email = tokenService.getResetEmailFromRedis(request.token());
//		userService.checkEmailOwnership(userDetail, email);
//		userService.changeUserPassword(userDetail, request.newPassword());
//		return ApiResponse.success(null);
//	}
}
