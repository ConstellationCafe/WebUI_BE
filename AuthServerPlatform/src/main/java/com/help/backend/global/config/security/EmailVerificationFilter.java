package com.help.backend.global.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.help.authserver.global.common.exception.ErrorCode;
import com.help.authserver.global.common.response.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Component
@Slf4j
@RequiredArgsConstructor
public class EmailVerificationFilter extends OncePerRequestFilter {
	// TODO: 이메일 정책 추가 and null 처리
	private static final List<String> TRUSTED_EMAIL_SERVERS = List.of(
		"https://mail.google.com",
		"https://mail.naver.com",
		"https://outlook.live.com",
		"http://localhost:5173",
		"http://www.tuzain.com/"
	);

	private final ObjectMapper objectMapper;

	@Override
	protected boolean shouldNotFilter(final HttpServletRequest request) {
		return !request.getRequestURI().startsWith("/api/auth/verify");
	}

	@Override
	protected void doFilterInternal(
		@NonNull final HttpServletRequest request,
		@NonNull final HttpServletResponse response,
		@NonNull final FilterChain filterChain) throws ServletException, IOException {
		final String referer = request.getHeader("Referer");
		final String origin = request.getHeader("Origin");
		log.info("이메일 검증 필터 동작 - Referer: {}, Origin: {}", referer, origin);

		if (null == referer) {
			filterChain.doFilter(request, response);
			return;
		}

		if (TRUSTED_EMAIL_SERVERS.stream().noneMatch(Objects.requireNonNull(referer)::startsWith)) {
			sendErrorResponse(response);
			return;
		}

		filterChain.doFilter(request, response);
	}

	private void sendErrorResponse(final HttpServletResponse response) throws IOException {
		response.setStatus(ErrorCode.UNTRUSTED_EMAIL_SERVER.getStatus());
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");

		final ApiResponse<?> errorResponse = ApiResponse.error(ErrorCode.UNTRUSTED_EMAIL_SERVER);
		final String jsonResponse = objectMapper.writeValueAsString(errorResponse);

		response.getWriter().write(jsonResponse);
	}
}
