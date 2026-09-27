package com.help.authserver.domain.user.controller;

import com.help.authserver.domain.user.service.DiscordLoginService;
import com.help.global.jwt.CustomUser;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.help.global.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;

/**
 * Discord OAuth2 로그인 및 Discord 부가 정보(내 정보, 참여 길드 목록) 전용 컨트롤러.
 * 세션/토큰 발급 이후의 일반 로그인 세션 관리(guild 선택, refresh, check, logout)는
 * {@link AuthController}가 담당한다.
 */
@RestController
@RequestMapping("/auth")
@Slf4j
@RequiredArgsConstructor
public class DiscordAuthController {
	private final DiscordLoginService discordLoginService;

	// 로그인
	@GetMapping("/discord_login")
	public ResponseEntity<Void> loginWithDiscord(
			@RequestParam("code") String code,  // OAuth2 발급 요청에 쓰는 코드
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
}
