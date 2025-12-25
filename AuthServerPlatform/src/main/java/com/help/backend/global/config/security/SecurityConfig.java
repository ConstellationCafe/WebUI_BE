package com.help.backend.global.config.security;

import com.help.authserver.global.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final EmailVerificationFilter emailVerificationFilter;

	// TODO: CSRF 토큰 사용 시 주석 해제
	// @Bean
	// public CsrfTokenRepository csrfTokenRepository() {
	// 	HttpSessionCsrfTokenRepository repository = new HttpSessionCsrfTokenRepository();
	// 	repository.setHeaderName("X-XSRF-TOKEN");
	// 	return repository;
	// }

	@Bean
	public SecurityFilterChain filterChain(final HttpSecurity http) throws Exception {
		// http.csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository())
		// 	.ignoringRequestMatchers("/login")
		// )
		http.csrf(AbstractHttpConfigurer::disable);
		http.sessionManagement(session -> session
			.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
		);
		http.cors(corsCustomizer -> corsCustomizer.configurationSource(configurationSource()));
		http.formLogin(AbstractHttpConfigurer::disable);
		http.authorizeHttpRequests((authorize) ->
			authorize.requestMatchers("/**").permitAll()
				.anyRequest().authenticated()
		);
		http.addFilterBefore(jwtAuthenticationFilter, ExceptionTranslationFilter.class)
			.addFilterBefore(emailVerificationFilter, JwtAuthenticationFilter.class);
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
		// configuration.setAllowedOrigins(List.of(
		// 	"http://192.168.25.93:5173"
		// ));
		configuration.setAllowedOriginPatterns(List.of("*"));
		
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
		configuration.setExposedHeaders(List.of("Authorization"));
		configuration.setAllowCredentials(true);

		final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);

		return source;
	}
}
