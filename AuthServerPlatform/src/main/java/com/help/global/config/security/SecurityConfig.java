package com.help.global.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * 모듈 공용 보안 설정.
 * <p>
 * ADR-0007: 경로별 보안 체인은 그 경로를 소유한 모듈이 가진다.
 * <ul>
 *   <li>{@code @Order(0)} {@code /api/integrations/**} — {@code global.external.ExternalSecurityConfig}</li>
 *   <li>{@code @Order(1)} {@code /auth/**} — {@code authserver.security.AuthServerSecurityConfig}</li>
 *   <li>{@code @Order(2)} {@code /api/**} — {@code erpweb.security.ErpWebSecurityConfig}</li>
 *   <li>{@code @Order(3)} {@code /actuator/**} — 이 클래스</li>
 * </ul>
 * 이 클래스는 authserver·erpweb에 의존하지 않고, 공용 bean(CORS, PasswordEncoder)과
 * actuator 체인만 둔다.
 */
@Configuration
@EnableMethodSecurity
@EnableWebSecurity
public class SecurityConfig {
	/** 모듈별 체인이 {@code @Qualifier}로 주입받는 공용 CORS 설정 bean 이름. */
	public static final String CORS_CONFIGURATION_SOURCE = "configurationSource";

	@Value("${front.redirect-uri}")
	private String redirectUri;

	 @Bean
	 public CsrfTokenRepository csrfTokenRepository() {
	 	HttpSessionCsrfTokenRepository repository = new HttpSessionCsrfTokenRepository();
	 	repository.setHeaderName("X-XSRF-TOKEN");
	 	return repository;
	 }

	@Bean
	@Order(3)
	public SecurityFilterChain actuatorChain(HttpSecurity http) throws Exception {
		http.securityMatcher("/actuator/**");
		http.csrf(AbstractHttpConfigurer::disable);
		http.sessionManagement(session ->
			session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
		);
		http.authorizeHttpRequests(auth -> auth
			.requestMatchers(
				"/actuator/health",
				"/actuator/health/liveness",
				"/actuator/health/readiness"
			).permitAll()
			.anyRequest().denyAll()
		);
		return http.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean(CORS_CONFIGURATION_SOURCE)
	public CorsConfigurationSource configurationSource() {
		final CorsConfiguration configuration = new CorsConfiguration();

		// 프론트엔드 서버 주소
		configuration.setAllowedOriginPatterns(List.of(redirectUri));

		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
		configuration.setExposedHeaders(List.of("Authorization"));
		configuration.setAllowCredentials(true);

		final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);

		return source;
	}
}
