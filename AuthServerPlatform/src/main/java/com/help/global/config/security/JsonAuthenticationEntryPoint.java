package com.help.global.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.help.global.common.exception.ErrorCode;
import com.help.global.common.response.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * ADR-0001 부수 수정: backendChain("/api/**")에는 원래 커스텀
 * AuthenticationEntryPoint가 없어서, AuthorizationFilter가 인증 실패로
 * 판단한 요청(예: 30초 AccessToken 만료)이 Spring Security 기본값인
 * Http403ForbiddenEntryPoint를 타고 본문 없는 403으로 응답했다.
 * 이 403은 ExceptionTranslationFilter 단계(서블릿 필터 체인)에서
 * 끝나버려서 DispatcherServlet/GlobalExceptionHandler까지 도달하지
 * 않고, 컨트롤러 로그도 전혀 찍히지 않는다.
 *
 * 그 결과 FE의 AuthInterceptor가 401에만 refresh-retry를 걸도록
 * 바뀐 뒤로, AccessToken 만료 상황이 이 "말 없는 403"으로 나가면서
 * 재시도가 걸리지 않는 회귀가 생겼다(/api/repository/membership/point_log
 * 버그의 원인).
 *
 * 이 EntryPoint는 인증 자체가 안 된 요청(토큰 없음/만료/위조)에 한해
 * ErrorCode.UNAUTHORIZED(401)로 응답을 통일한다. 반대로 인증은 됐지만
 * 권한이 없는 경우(GUILD_MEMBER_NOT_FOUND 등)는 서비스 계층에서
 * CustomException으로 던져 GlobalExceptionHandler가 그대로 403으로
 * 처리하므로 이 EntryPoint를 타지 않는다 — 403은 이제 진짜
 * 비즈니스 로직 거부에만 쓰이게 된다.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ObjectMapper objectMapper;

	@Override
	public void commence(
		final HttpServletRequest request,
		final HttpServletResponse response,
		final AuthenticationException authException
	) throws IOException, ServletException {
		log.warn(
			"인증되지 않은 /api 요청 - uri={}, reason={}",
			request.getRequestURI(),
			authException.getMessage()
		);

		response.setStatus(ErrorCode.UNAUTHORIZED.getStatus());
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");

		final ApiResponse<?> errorResponse = ApiResponse.error(ErrorCode.UNAUTHORIZED);
		final String jsonResponse = objectMapper.writeValueAsString(errorResponse);

		response.getWriter().write(jsonResponse);
	}
}
