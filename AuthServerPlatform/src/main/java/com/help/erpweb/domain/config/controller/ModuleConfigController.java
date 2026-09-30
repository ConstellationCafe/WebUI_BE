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

/** JWT 필터가 검증한 채팅방만 조회한다. 클라이언트의 botId는 받지 않는다. */
@RestController
@RequestMapping("/api/me/module-configs")
@RequiredArgsConstructor
public class ModuleConfigController {
	private final ModuleConfigService service;

	@GetMapping
	public ApiResponse<List<ModuleConfigResponse>> getMenuConfigs() {
		return ApiResponse.success(service.getMenuConfigs(GuildContext.requireBotId()));
	}
}
