package com.help.erpweb.domain.config.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.help.erpweb.domain.config.dto.response.ModuleConfigResponse;
import com.help.erpweb.domain.config.service.ModuleConfigService;
import com.help.global.discord.identity.DiscordUser;
import com.help.global.discord.identity.DiscordUserRepository;
import com.help.global.guild.GuildContext;
import com.help.global.jwt.JwtUtil;

import jakarta.servlet.http.Cookie;

/** 실제 SecurityConfig·JWT 필터를 통해 인증·방 선택·응답 계약을 함께 검증한다. */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ModuleConfigControllerTestConfig.class)
@TestPropertySource(properties = "front.redirect-uri=https://example.invalid")
@WebAppConfiguration
class ModuleConfigControllerTest {
	@Autowired
	private WebApplicationContext context;
	@Autowired
	private ModuleConfigService service;
	@Autowired
	private JwtUtil jwtUtil;
	@Autowired
	private DiscordUserRepository users;
	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		reset(service, jwtUtil, users);
		mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
		when(jwtUtil.extractAccessTokenFromRequest(any())).thenAnswer(invocation -> {
			Cookie[] cookies = ((jakarta.servlet.http.HttpServletRequest)invocation.getArgument(0)).getCookies();
			return cookies == null ? Optional.empty() : Optional.of(cookies[0].getValue());
		});
		when(jwtUtil.isTokenValidate("scoped-token")).thenReturn(true);
		when(jwtUtil.extractBotId("scoped-token")).thenReturn(Optional.of("bot-a"));
		when(jwtUtil.extractUsername("scoped-token")).thenReturn(Optional.of("123"));
		when(users.findByBotIdAndDiscordID("bot-a", "123"))
				.thenReturn(Optional.of(DiscordUser.of("bot-a", "123", List.of())));
	}

	@AfterEach
	void clearContext() {
		SecurityContextHolder.clearContext();
		GuildContext.clear();
	}

	@Test
	void unauthenticatedRequestIsUnauthorized() throws Exception {
		mvc.perform(get("/api/bots/current/module-configs")).andExpect(status().isUnauthorized());
		verifyNoInteractions(service);
	}

	@Test
	void tokenWithoutSelectedBotIsUnauthorized() throws Exception {
		when(jwtUtil.isTokenValidate("pending-token")).thenReturn(true);
		when(jwtUtil.extractBotId("pending-token")).thenReturn(Optional.empty());
		mvc.perform(get("/api/bots/current/module-configs").cookie(new Cookie("AccessToken", "pending-token")))
				.andExpect(status().isUnauthorized());
		verifyNoInteractions(service);
	}

	@Test
	void requestCannotOverrideJwtBotAndNonAdminMemberCanReadMenuConfig() throws Exception {
		when(service.getMenuConfigs("bot-a"))
				.thenReturn(List.of(new ModuleConfigResponse("network_operations", List.of("competition"))));

		mvc.perform(get("/api/bots/current/module-configs")
						.cookie(new Cookie("AccessToken", "scoped-token"))
						.param("botId", "bot-b").param("bot_id", "bot-b"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.response[0].moduleId").value("network_operations"))
				.andExpect(jsonPath("$.response[0].addOns[0]").value("competition"))
				.andExpect(jsonPath("$.response[0].config").doesNotExist());
		verify(service).getMenuConfigs("bot-a");
		assertThatThrownBy(GuildContext::requireBotId).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void sameBotReturnsSameConfigurationForDifferentUsersAndRoles() throws Exception {
		when(jwtUtil.isTokenValidate("other-user-token")).thenReturn(true);
		when(jwtUtil.extractBotId("other-user-token")).thenReturn(Optional.of("bot-a"));
		when(jwtUtil.extractUsername("other-user-token")).thenReturn(Optional.of("456"));
		when(users.findByBotIdAndDiscordID("bot-a", "456"))
				.thenReturn(Optional.of(DiscordUser.of("bot-a", "456", List.of("서버장"))));
		when(service.getMenuConfigs("bot-a"))
				.thenReturn(List.of(new ModuleConfigResponse("chatbot", List.of())));

		String memberResponse = mvc.perform(get("/api/bots/current/module-configs")
						.cookie(new Cookie("AccessToken", "scoped-token")))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		String adminResponse = mvc.perform(get("/api/bots/current/module-configs")
						.cookie(new Cookie("AccessToken", "other-user-token")))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

		assertThat(adminResponse).isEqualTo(memberResponse);
	}

	@Test
	void sameUserReceivesConfigurationOfSelectedBotOnly() throws Exception {
		when(jwtUtil.isTokenValidate("other-bot-token")).thenReturn(true);
		when(jwtUtil.extractBotId("other-bot-token")).thenReturn(Optional.of("bot-b"));
		when(jwtUtil.extractUsername("other-bot-token")).thenReturn(Optional.of("123"));
		when(users.findByBotIdAndDiscordID("bot-b", "123"))
				.thenReturn(Optional.of(DiscordUser.of("bot-b", "123", List.of())));
		when(service.getMenuConfigs("bot-a"))
				.thenReturn(List.of(new ModuleConfigResponse("chatbot", List.of())));
		when(service.getMenuConfigs("bot-b"))
				.thenReturn(List.of(new ModuleConfigResponse("shadowverse", List.of())));

		mvc.perform(get("/api/bots/current/module-configs")
						.cookie(new Cookie("AccessToken", "scoped-token")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.response[0].moduleId").value("chatbot"));
		mvc.perform(get("/api/bots/current/module-configs")
						.cookie(new Cookie("AccessToken", "other-bot-token")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.response[0].moduleId").value("shadowverse"));
		verify(service).getMenuConfigs("bot-a");
		verify(service).getMenuConfigs("bot-b");
	}

	@Test
	void previousUserResourcePathIsNotExposed() throws Exception {
		mvc.perform(get("/api/me/module-configs").cookie(new Cookie("AccessToken", "scoped-token")))
				.andExpect(status().isNotFound());
		verifyNoInteractions(service);
	}

	@Test
	void userOutsideSelectedRoomCannotReadModuleConfig() throws Exception {
		when(users.findByBotIdAndDiscordID("bot-a", "123")).thenReturn(Optional.empty());
		mvc.perform(get("/api/bots/current/module-configs").cookie(new Cookie("AccessToken", "scoped-token")))
				.andExpect(status().isUnauthorized());
		verifyNoInteractions(service);
	}
}
