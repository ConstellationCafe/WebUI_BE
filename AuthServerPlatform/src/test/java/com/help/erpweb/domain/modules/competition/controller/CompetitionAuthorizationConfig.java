package com.help.erpweb.domain.modules.competition.controller;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import com.help.erpweb.domain.modules.competition.service.CompetitionService;
import com.help.global.authorization.Authorization;

@Configuration
@EnableMethodSecurity
public class CompetitionAuthorizationConfig {
	@Bean
	public CompetitionService competitionService() {
		return Mockito.mock(CompetitionService.class);
	}

	@Bean
	public Authorization authorization() {
		return new Authorization();
	}

	@Bean
	public AdminCompetitionController adminCompetitionController(CompetitionService service) {
		return new AdminCompetitionController(service);
	}
}
