package com.help.erpweb.domain.config.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.help.erpweb.domain.config.dto.response.ModuleConfigResponse;
import com.help.erpweb.domain.config.entity.ModuleConfig;
import com.help.erpweb.domain.config.entity.ModuleConfigId;
import com.help.erpweb.domain.config.repository.ModuleConfigRepository;

class ModuleConfigServiceTest {
	private final ObjectMapper mapper = new ObjectMapper();
	private final ModuleConfigRepository repository = mock(ModuleConfigRepository.class);
	private final ModuleConfigService service = new ModuleConfigService(repository);

	@Test
	void enabledMenusComeFromCurrentBotModulesAndNetworkAddOns() throws Exception {
		when(repository.findByIdBotIdAndIdModuleIdInOrderByIdModuleIdAsc(eq("bot-a"), anyCollection()))
				.thenReturn(List.of(
						module("chatbot", "null"),
						module("network_operations", """
								{"add_on":{"point":{},"academy":{"guild_id":123},"competition":{}},
								"membership":{"welcome_messages":["server-only configuration"]}}
								"""),
						module("shadowverse", "{}")
				));

		List<ModuleConfigResponse> result = service.getMenuConfigs("bot-a");

		assertThat(result).containsExactly(
				new ModuleConfigResponse("chatbot", List.of()),
				new ModuleConfigResponse("network_operations", List.of("academy", "competition")),
				new ModuleConfigResponse("shadowverse", List.of())
		);
		verify(repository).findByIdBotIdAndIdModuleIdInOrderByIdModuleIdAsc(
				"bot-a", List.of("chatbot", "network_operations", "shadowverse"));
		assertThat(mapper.writeValueAsString(result))
				.doesNotContain("guild_id", "welcome_messages", "server-only configuration", "point", "config");
	}

	@Test
	void missingModulesReturnEmptyList() {
		when(repository.findByIdBotIdAndIdModuleIdInOrderByIdModuleIdAsc(eq("bot-b"), anyCollection()))
				.thenReturn(List.of());

		assertThat(service.getMenuConfigs("bot-b")).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"null", "{}", "[]", "true", "{\"add_on\":null}",
		"{\"add_on\":{\"academy\":null,\"competition\":false}}",
		"{\"academy\":{},\"competition\":{}}"
	})
	void missingNullOrMalformedNetworkAddOnsDoNotEnableMenus(String config) throws Exception {
		when(repository.findByIdBotIdAndIdModuleIdInOrderByIdModuleIdAsc(eq("bot-a"), anyCollection()))
				.thenReturn(List.of(module("network_operations", config)));

		assertThat(service.getMenuConfigs("bot-a").get(0).addOns()).isEmpty();
	}

	@Test
	void onlyObjectAddOnsUnderNetworkOperationsAreEnabled() throws Exception {
		when(repository.findByIdBotIdAndIdModuleIdInOrderByIdModuleIdAsc(eq("bot-a"), anyCollection()))
				.thenReturn(List.of(
						module("chatbot", "{\"add_on\":{\"competition\":{}}}"),
						module("network_operations", "{\"add_on\":{\"academy\":{},\"competition\":null}}")
				));

		assertThat(service.getMenuConfigs("bot-a")).containsExactly(
				new ModuleConfigResponse("chatbot", List.of()),
				new ModuleConfigResponse("network_operations", List.of("academy"))
		);
	}

	private ModuleConfig module(String moduleId, String config) throws Exception {
		ModuleConfig module = new ModuleConfig();
		ReflectionTestUtils.setField(module, "id", new ModuleConfigId("bot-a", moduleId));
		ReflectionTestUtils.setField(module, "config", mapper.readTree(config));
		return module;
	}
}
