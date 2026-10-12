package com.help.authserver.security;

import com.help.global.config.security.SecurityConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * authserver {@code /auth/**} 보안 체인.
 * <p>
 * ADR-0007: 보안 체인은 경로를 소유한 모듈이 가진다. 이 체인은 authserver의 로그인 세션에
 * 의존하는 {@link AuthServerJwtAuthFilter}를 쓰므로 global이 아니라 authserver에 둔다.
 */
@Configuration
public class AuthServerSecurityConfig {

	@Bean
	@Order(1)
	public SecurityFilterChain authServerChain(
		final HttpSecurity http,
		final AuthServerJwtAuthFilter authServerJwtAuthFilter,
		@Qualifier(SecurityConfig.CORS_CONFIGURATION_SOURCE) final CorsConfigurationSource corsConfigurationSource
	) throws Exception {
		http.securityMatcher("/auth/**");

		http.csrf(AbstractHttpConfigurer::disable);  // CSRF 비활성화 (JWT 기반)
		http.sessionManagement(session -> session
			.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
		);  // 세션 생성 x, JWT로만 인증
		http.cors(corsCustomizer -> corsCustomizer.configurationSource(corsConfigurationSource));  // 커스텀 CORS
		http.formLogin(AbstractHttpConfigurer::disable);  // login 폼 기반 인증 x

		http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());  // 인가는 필터에서 처리
		http.addFilterBefore(authServerJwtAuthFilter, ExceptionTranslationFilter.class);
		return http.build();
	}
}
