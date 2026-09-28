package com.help.global.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.help.global.common.exception.ErrorCode;
import com.help.global.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * ADR-0002: backendChain의 URL 인가 규칙(/api/admin/** → ROLE_ADMIN)에서
 * 거부된 요청을 처리한다.
 * <p>
 * 인증은 됐지만 권한이 없는 사용자에게는 {@code GlobalExceptionHandler}의
 * method authorization 거부 처리와 같은 {@link ErrorCode#NOT_FOUND}(404)를
 * 돌려준다. 관리자 API의 존재 여부를 일반 사용자에게 드러내지 않기 위함이며,
 * 이 핸들러가 없으면 Spring Security 기본값이 본문 없는 403을 응답한다.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

	private final ObjectMapper objectMapper;

	@Override
	public void handle(
		final HttpServletRequest request,
		final HttpServletResponse response,
		final AccessDeniedException accessDeniedException
	) throws IOException {
		log.warn("권한 없는 /api 요청 - uri={}", request.getRequestURI());

		response.setStatus(ErrorCode.NOT_FOUND.getStatus());
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(
			objectMapper.writeValueAsString(ApiResponse.error(ErrorCode.NOT_FOUND))
		);
	}
}
