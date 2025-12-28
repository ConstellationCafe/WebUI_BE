package com.help.global.config.security;

import com.help.global.jwt.AuthServerJwtAuthFilter;
import com.help.global.jwt.BackEndJwtAuthFilter;
import lombok.RequiredArgsConstructor;
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
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
	private final AuthServerJwtAuthFilter authServerJwtAuthFilter;
	private final BackEndJwtAuthFilter backEndJwtAuthFilter;
	private final EmailVerificationFilter emailVerificationFilter;

	@Value("${front.redirect-uri}")
	private String redirectUri;

	 @Bean
	 public CsrfTokenRepository csrfTokenRepository() {
	 	HttpSessionCsrfTokenRepository repository = new HttpSessionCsrfTokenRepository();
	 	repository.setHeaderName("X-XSRF-TOKEN");
	 	return repository;
	 }

	@Bean
	@Order(1)
	public SecurityFilterChain filterChain(final HttpSecurity http) throws Exception {
		http.securityMatcher("/auth/**");

		http.csrf(AbstractHttpConfigurer::disable);  // CSRF 비활성화 (JWT 기반)
		http.sessionManagement(session -> session
			.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
		);  // 세션 생성 x, JWT로만 인증
		http.cors(corsCustomizer -> corsCustomizer.configurationSource(configurationSource()));  // 커스텀 CORS
		http.formLogin(AbstractHttpConfigurer::disable);  // login 폼 기반 인증 x

		http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());  // 인가 설정
		http.addFilterBefore(authServerJwtAuthFilter, ExceptionTranslationFilter.class)
			.addFilterBefore(emailVerificationFilter, AuthServerJwtAuthFilter.class);  // 인증 필터 구성
		return http.build();
	}

	@Bean
	@Order(2)
	public SecurityFilterChain backendChain(HttpSecurity http) throws Exception {
		http.securityMatcher("/api/**");

		http.csrf(AbstractHttpConfigurer::disable);
		http.sessionManagement(session ->
				session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
		);

		http.addFilterBefore(backEndJwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

		http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
		return http.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	public CorsConfigurationSource configurationSource() {
		final CorsConfiguration configuration = new CorsConfiguration();

		// 프론트엔드 서버 주소
		configuration.setAllowedOriginPatterns(List.of(redirectUri));
		
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
		configuration.setExposedHeaders(List.of("Authorization"));
		configuration.setAllowCredentials(true);

		final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);

		return source;
	}
}
