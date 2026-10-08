package com.help.erpweb.domain.modules.erp.penalty.controller;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import com.help.erpweb.domain.modules.erp.penalty.authorization.PenaltyAuthorization;
import com.help.erpweb.domain.modules.erp.penalty.service.PenaltyService;
import com.help.global.discord.identity.DiscordUserRepository;

@Configuration
@EnableMethodSecurity
public class PenaltyAuthorizationConfig {
	@Bean
	public PenaltyService penaltyService() {
		return Mockito.mock(PenaltyService.class);
	}

	@Bean
	public DiscordUserRepository discordUserRepository() {
		return Mockito.mock(DiscordUserRepository.class);
	}

	@Bean(name = "penaltyAuth")
	public PenaltyAuthorization penaltyAuth(DiscordUserRepository repository) {
		return new PenaltyAuthorization(repository);
	}

	@Bean
	public PenaltyController penaltyController(PenaltyService service) {
		return new PenaltyController(service);
	}

	@Bean
	public PenaltyPermissionController penaltyPermissionController(PenaltyAuthorization authorization) {
		return new PenaltyPermissionController(authorization);
	}
}
