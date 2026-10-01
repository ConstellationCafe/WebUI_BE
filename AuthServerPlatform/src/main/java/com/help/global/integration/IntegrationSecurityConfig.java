package com.help.global.integration;

import com.help.global.config.security.JsonAccessDeniedHandler;
import com.help.global.config.security.JsonAuthenticationEntryPoint;
import com.help.global.data.Authority;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 외부 시스템용 {@code /api/integrations/**} 보안 체인(ADR-0004).
 * <p>
 * 사용자 JWT가 아니라 서비스 API Key만 인정한다. {@code /api/**} backendChain보다 먼저
 * 매칭되어야 하므로 더 높은 우선순위를 준다. 서버 간 호출이므로 CORS를 열지 않는다.
 */
@Configuration
@EnableConfigurationProperties(IntegrationClientProperties.class)
public class IntegrationSecurityConfig {

    @Bean
    @Order(0)
    public SecurityFilterChain integrationChain(
            HttpSecurity http,
            IntegrationClientProperties properties,
            JsonAuthenticationEntryPoint authenticationEntryPoint,
            JsonAccessDeniedHandler accessDeniedHandler
    ) throws Exception {
        http.securityMatcher("/api/integrations/**");
        http.csrf(AbstractHttpConfigurer::disable);
        http.formLogin(AbstractHttpConfigurer::disable);
        http.httpBasic(AbstractHttpConfigurer::disable);
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.addFilterBefore(
                new IntegrationApiKeyAuthenticationFilter(properties),
                UsernamePasswordAuthenticationFilter.class
        );
        http.authorizeHttpRequests(auth -> auth.anyRequest().hasAuthority(Authority.INTEGRATION));
        http.exceptionHandling(exceptionHandling -> exceptionHandling
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
        );
        return http.build();
    }
}
