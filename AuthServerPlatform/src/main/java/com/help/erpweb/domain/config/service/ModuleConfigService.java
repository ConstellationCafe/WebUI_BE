package com.help.erpweb.domain.config.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.help.erpweb.domain.config.dto.response.ModuleConfigResponse;
import com.help.erpweb.domain.config.entity.ModuleConfig;
import com.help.erpweb.domain.config.repository.ModuleConfigRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ModuleConfigService {
	// 복합 PK 때문에 메뉴용 모듈은 최대 세 행이다. 설정 원문은 응답에 포함하지 않는다.
	private static final List<String> MENU_MODULES = List.of("chatbot", "network_operations", "shadowverse");
	private static final List<String> MENU_ADD_ONS = List.of("academy", "competition");

	private final ModuleConfigRepository repository;

	@Transactional(transactionManager = "configDBTransactionManager", readOnly = true)
	public List<ModuleConfigResponse> getMenuConfigs(String botId) {
		return repository.findByIdBotIdAndIdModuleIdInOrderByIdModuleIdAsc(botId, MENU_MODULES).stream()
				.map(module -> new ModuleConfigResponse(module.getModuleId(), menuAddOns(module)))
				.toList();
	}

	private static List<String> menuAddOns(ModuleConfig module) {
		JsonNode config = module.getConfig();
		if (!"network_operations".equals(module.getModuleId()) || config == null) {
			return List.of();
		}
		JsonNode addOns = config.path("add_on");
		return MENU_ADD_ONS.stream().filter(name -> addOns.path(name).isObject()).toList();
	}
}
