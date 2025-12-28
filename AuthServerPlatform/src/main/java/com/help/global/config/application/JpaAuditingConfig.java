package com.help.global.config.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
@ConditionalOnProperty(name = "app.jpa-auditing", havingValue = "true", matchIfMissing = true)
class JpaAuditingConfig {}
