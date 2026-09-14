package com.help.erpweb.domain.config.repository;

import com.help.erpweb.domain.config.entity.ModuleConfig;
import com.help.erpweb.domain.config.entity.ModuleConfigId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ModuleConfigRepository
        extends JpaRepository<ModuleConfig, ModuleConfigId> {

    Optional<ModuleConfig> findByIdBotIdAndIdModuleId(
            String botId,
            String moduleId
    );
}