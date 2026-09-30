package com.help.erpweb.domain.config.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.help.erpweb.domain.config.dto.response.ModuleConfigResponse;
import com.help.erpweb.domain.config.service.ModuleConfigService;
import com.help.global.common.response.ApiResponse;
import com.help.global.guild.GuildContext;

import lombok.RequiredArgsConstructor;

/** current는 JWT로 선택한 봇이다. 설정은 사용자·역할과 무관하게 botId만으로 결정된다. */
@RestController
@RequestMapping("/api/bots/current/module-configs")
@RequiredArgsConstructor
public class ModuleConfigController {
	private final ModuleConfigService service;

	@GetMapping
	public ApiResponse<List<ModuleConfigResponse>> getMenuConfigs() {
		return ApiResponse.success(service.getMenuConfigs(GuildContext.requireBotId()));
	}
}
