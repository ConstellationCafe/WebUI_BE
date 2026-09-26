package com.help.authserver.domain.user.controller;

import com.help.authserver.domain.user.dto.request.GuildSelectRequestDto;
import com.help.authserver.domain.user.dto.response.LoginCheckResponseDto;
import com.help.authserver.domain.user.service.AuthSessionService;
import com.help.authserver.domain.user.service.DiscordLoginService;
import com.help.global.jwt.CustomUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

//import com.help.authserver.domain.user.service.AuthService;
import com.help.global.common.response.ApiResponse;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;

@RestController
@RequestMapping("/auth")
@Slf4j
@RequiredArgsConstructor
public class AuthController {
//	private final UserService userService;
//	private final AuthService authService;  // 형식상 남겨둠
	private final DiscordLoginService discordLoginService;
	private final AuthSessionService authSessionService;
//	private final VerificationTokenService tokenService;

	// 로그인 (형식상 남겨둠)
//	@PostMapping("/login")
//	public ApiResponse<?> login(
//		@Valid @RequestBody final LoginRequestDto loginRequestDto,
//		@RequestParam(value = "redirectUrl", defaultValue = "/") final String redirectUrl,
//		final HttpServletResponse response
//	) {
//		return authService.login(loginRequestDto, redirectUrl, response);
//	}

	// 로그인
	@GetMapping("/discord_login")
	public ResponseEntity<Void> loginWithDiscord(
			@RequestParam("code") String code,  // OAuth2 발급 요청에 쓰는 코드
//			@RequestParam(value = "state", required = false) String state,
			HttpServletResponse response
	) {
		String redirectTo = discordLoginService.login(code, response);
		HttpHeaders headers = new HttpHeaders();
		headers.setLocation(URI.create(redirectTo));
		log.info("로그인 성공, 리다이렉션 시작");
		return new ResponseEntity<>(headers, HttpStatus.FOUND); // 302
	}

	@GetMapping("/me")
	public ApiResponse<?> me(
		@AuthenticationPrincipal CustomUser user
	) {
		log.info("[GET] /auth/me");
		return discordLoginService.me(user);
	}

	@GetMapping("/guilds")
	public ApiResponse<?> guilds(
		@AuthenticationPrincipal CustomUser user
	) {
		log.info("[GET] /auth/guilds");
		return discordLoginService.guilds(user);
	}

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
