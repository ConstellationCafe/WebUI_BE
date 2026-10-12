package com.help.erpweb.domain.modules.competition.controller;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import com.help.erpweb.domain.modules.competition.authorization.CompetitionAuthorization;
import com.help.erpweb.domain.modules.competition.service.CompetitionService;
import com.help.global.discord.identity.DiscordUserRepository;

@Configuration
@EnableMethodSecurity
public class CompetitionAuthorizationConfig {
	@Bean
	public CompetitionService competitionService() {
		return Mockito.mock(CompetitionService.class);
	}

	@Bean
	public DiscordUserRepository discordUserRepository() {
		return Mockito.mock(DiscordUserRepository.class);
	}

	@Bean(name = "competitionAuth")
	public CompetitionAuthorization competitionAuth(DiscordUserRepository repository) {
		return new CompetitionAuthorization(repository);
	}

	@Bean
	public CompetitionController competitionController(
		CompetitionService service,
		CompetitionAuthorization authorization
	) {
		return new CompetitionController(service, authorization);
	}
}
