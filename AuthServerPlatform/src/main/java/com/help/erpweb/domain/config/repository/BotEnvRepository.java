package com.help.erpweb.domain.config.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.help.erpweb.domain.config.entity.BotEnv;

public interface BotEnvRepository extends JpaRepository<BotEnv, String> {
}
