package com.help.erpweb.domain.modules.erp.penalty.controller;

import com.help.erpweb.domain.modules.erp.penalty.service.PenaltyService;
import com.help.global.authorization.Authorization;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableMethodSecurity
public class PenaltyAuthorizationConfig {
    @Bean
    public PenaltyService penaltyService() {
        return Mockito.mock(PenaltyService.class);
    }

    @Bean
    public Authorization authorization() {
        return new Authorization();
    }

    @Bean
    public AdminPenaltyController adminPenaltyController(PenaltyService service) {
        return new AdminPenaltyController(service);
    }
}
