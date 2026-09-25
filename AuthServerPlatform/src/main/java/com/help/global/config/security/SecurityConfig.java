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
	private final JsonAuthenticationEntryPoint jsonAuthenticationEntryPoint;

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

		http.csrf(AbstractHttpConfigurer::disable);  // CSRF ë¹íì±í (JWT ê¸°ë°)
		http.sessionManagement(session -> session
			.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
		);  // ì¸ì ìì± x, JWTë¡ë§ ì¸ì¦
		http.cors(corsCustomizer -> corsCustomizer.configurationSource(configurationSource()));  // ì»¤ì¤í CORS
		http.formLogin(AbstractHttpConfigurer::disable);  // login í¼ ê¸°ë° ì¸ì¦ x

		http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());  // ì¸ê° ì¤ì 
		http.addFilterBefore(authServerJwtAuthFilter, ExceptionTranslationFilter.class)
			.addFilterBefore(emailVerificationFilter, AuthServerJwtAuthFilter.class);  // ì¸ì¦ íí° êµ¬ì±
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

		// ADR-0001 부수 수정: 커스텀 AuthenticationEntryPoint가 없으면 Spring
		// Security 기본값(Http403ForbiddenEntryPoint)이 인증 실패를 본문 없는
		// 403으로 응답해서, AccessToken 만료가 컨트롤러/GlobalExceptionHandler
		// 로그도 없이 조용히 403으로 나가버린다(FE AuthInterceptor의 401-only
		// refresh-retry와 충돌). 인증 실패는 항상 401로 통일한다.
		http.exceptionHandling(exceptionHandling ->
			exceptionHandling.authenticationEntryPoint(jsonAuthenticationEntryPoint)
		);

		return http.build();
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

	@Bean
	public CorsConfigurationSource configurationSource() {
		final CorsConfiguration configuration = new CorsConfiguration();

		// íë¡ í¸ìë ìë² ì£¼ì
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
