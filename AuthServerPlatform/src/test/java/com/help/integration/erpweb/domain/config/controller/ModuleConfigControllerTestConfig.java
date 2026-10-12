package com.help.erpweb.domain.config.controller;

import static org.mockito.Mockito.mock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.help.authserver.security.AuthServerJwtAuthFilter;
import com.help.erpweb.domain.config.service.ModuleConfigService;
import com.help.global.config.security.JsonAccessDeniedHandler;
import com.help.global.config.security.JsonAuthenticationEntryPoint;
import com.help.global.config.security.SecurityConfig;
import com.help.global.discord.identity.DiscordUserRepository;
import com.help.global.jwt.BackEndJwtAuthFilter;
import com.help.global.jwt.JwtUtil;

@Configuration
@EnableWebMvc
@Import({
	SecurityConfig.class, ModuleConfigController.class,
	JsonAccessDeniedHandler.class, JsonAuthenticationEntryPoint.class
})
public class ModuleConfigControllerTestConfig {
	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper();
	}

	@Bean
	public ModuleConfigService moduleConfigService() {
		return mock(ModuleConfigService.class);
	}

	@Bean
	public JwtUtil jwtUtil() {
		return mock(JwtUtil.class);
	}

	@Bean
	public DiscordUserRepository discordUserRepository() {
		return mock(DiscordUserRepository.class);
	}

	@Bean
	public AuthServerJwtAuthFilter authServerJwtAuthFilter() {
		return mock(AuthServerJwtAuthFilter.class);
	}

	@Bean
	public BackEndJwtAuthFilter backEndJwtAuthFilter(JwtUtil jwtUtil, DiscordUserRepository users) {
		return new BackEndJwtAuthFilter(jwtUtil, users);
	}
}
