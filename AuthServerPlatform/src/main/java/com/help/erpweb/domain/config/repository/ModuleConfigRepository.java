package com.help.erpweb.domain.config.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.help.erpweb.domain.config.entity.ModuleConfig;
import com.help.erpweb.domain.config.entity.ModuleConfigId;

public interface ModuleConfigRepository
		extends JpaRepository<ModuleConfig, ModuleConfigId> {

	List<ModuleConfig> findByIdBotIdAndIdModuleIdInOrderByIdModuleIdAsc(
			String botId, Collection<String> moduleIds
	);

	Optional<ModuleConfig> findByIdBotIdAndIdModuleId(
			String botId,
			String moduleId
	);
}
