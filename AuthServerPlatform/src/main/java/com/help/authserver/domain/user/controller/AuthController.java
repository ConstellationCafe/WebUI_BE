package com.help.authserver.domain.user.controller;

import com.help.authserver.domain.user.dto.response.LoginCheckResponseDto;
import com.help.authserver.domain.user.service.DiscordAuthService;
import com.help.global.jwt.CustomUser;
import jakarta.servlet.http.HttpServletRequest;
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
	private final DiscordAuthService discordAuthService;
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
		String redirectTo = discordAuthService.login(code, response);
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
		return discordAuthService.me(user);
	}

	@GetMapping("/guilds")
	public ApiResponse<?> guilds(
		@AuthenticationPrincipal CustomUser user
	) {
		log.info("[GET] /auth/guilds");
		return discordAuthService.guilds(user);
	}

	// AccessToken 갱신 요청
	@PostMapping("/refresh")
	public ApiResponse<?> refresh(
		final HttpServletRequest request,
		final HttpServletResponse response
	) {
		log.info("[POST] /auth/refresh");
		return discordAuthService.refresh(request, response);
	}

	@GetMapping("/check")
	public ApiResponse<?> loginCheck(final HttpServletRequest request) {
		log.info("[GET] /auth/check");
		return discordAuthService.checkLogin(request);
	}

	@PostMapping("/logout")
	public ApiResponse<?> logout(
			@AuthenticationPrincipal CustomUser user,
			final HttpServletResponse response
	) {
		log.info("[GET] /auth/logout");
		discordAuthService.logout(user, response);
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
