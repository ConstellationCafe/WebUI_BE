package com.help.erpweb.security;

import com.help.global.config.security.JsonAccessDeniedHandler;
import com.help.global.config.security.JsonAuthenticationEntryPoint;
import com.help.global.config.security.SecurityConfig;
import com.help.global.data.Authority;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * erpweb {@code /api/**} 보안 체인.
 * <p>
 * ADR-0007: 보안 체인은 경로를 소유한 모듈이 가진다. global은 authserver·erpweb에
 * 의존하지 않고 공용 부품(CORS, JSON 401/404 응답, actuator 체인)만 제공한다.
 * {@code /api/integrations/**}(@Order(0))는 이 체인보다 먼저 매칭된다.
 */
@Configuration
public class ErpWebSecurityConfig {

	@Bean
	@Order(2)
	public SecurityFilterChain backendChain(
		final HttpSecurity http,
		final BackEndJwtAuthFilter backEndJwtAuthFilter,
		final JsonAuthenticationEntryPoint jsonAuthenticationEntryPoint,
		final JsonAccessDeniedHandler jsonAccessDeniedHandler,
		@Qualifier(SecurityConfig.CORS_CONFIGURATION_SOURCE) final CorsConfigurationSource corsConfigurationSource
	) throws Exception {
		http.securityMatcher("/api/**");

		http.csrf(AbstractHttpConfigurer::disable);
		http.sessionManagement(session ->
				session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
		);
		http.cors(cors -> cors.configurationSource(corsConfigurationSource));

		http.addFilterBefore(backEndJwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

		// ADR-0002: 관리자 전용 API는 /api/admin/** 아래에만 둔다. 컨트롤러의
		// @PreAuthorize와 별개로 여기서 한 번 더 막아, 어노테이션을 빠뜨려도
		// 관리자 API가 일반 사용자에게 열리지 않게 한다.
		http.authorizeHttpRequests(auth -> auth
			// ADR-0004: SSE(/api/me/notifications/stream)는 비동기 dispatch로 응답을 이어 쓴다.
			// JWT 필터는 비동기 dispatch에서 다시 돌지 않으므로, 최초 요청에서 이미 인가된
			// 응답의 후속 dispatch를 여기서 다시 막지 않는다.
			.dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
			.requestMatchers("/api/admin/**").hasAuthority(Authority.ADMIN)
			.anyRequest().authenticated()
		);

		// ADR-0001 부수 수정: 커스텀 AuthenticationEntryPoint가 없으면 Spring
		// Security 기본값(Http403ForbiddenEntryPoint)이 인증 실패를 본문 없는
		// 403으로 응답해서, AccessToken 만료가 컨트롤러/GlobalExceptionHandler
		// 로그도 없이 조용히 403으로 나가버린다(FE AuthInterceptor의 401-only
		// refresh-retry와 충돌). 인증 실패는 항상 401로 통일한다.
		http.exceptionHandling(exceptionHandling -> exceptionHandling
			.authenticationEntryPoint(jsonAuthenticationEntryPoint)
			.accessDeniedHandler(jsonAccessDeniedHandler)
		);

		return http.build();
	}
}
