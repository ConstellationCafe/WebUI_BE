package com.help.erpweb.domain.modules.competition.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

import com.help.erpweb.domain.modules.competition.dto.request.CompetitionCreateRequest;
import com.help.erpweb.domain.modules.competition.service.CompetitionService;
import com.help.global.discord.identity.DiscordUser;
import com.help.global.discord.identity.DiscordUserRepository;
import com.help.global.guild.GuildContext;
import com.help.global.jwt.CustomUser;

class CompetitionAuthorizationTest {
	private static final String BOT_ID = "bot-a";
	private AnnotationConfigApplicationContext context;
	private CompetitionController controller;
	private CompetitionService service;
	private DiscordUserRepository users;

	@BeforeEach
	void setUp() {
		context = new AnnotationConfigApplicationContext(CompetitionAuthorizationConfig.class);
		controller = context.getBean(CompetitionController.class);
		service = context.getBean(CompetitionService.class);
		users = context.getBean(DiscordUserRepository.class);
		GuildContext.setBotId(BOT_ID);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
		GuildContext.clear();
		context.close();
	}

	@Test
	void memberWithoutManagerRoleCannotUseCompetitionFeaturesBeforeServiceRuns() {
		CustomUser user = login("123", "ROLE_USER");
		givenRoles("123", List.of("본부원", "대회 구독"));

		assertThatThrownBy(controller::boards).isInstanceOf(AuthorizationDeniedException.class);
		assertThatThrownBy(() -> controller.post(new CompetitionCreateRequest("req-1", "inner_board", null), user))
			.isInstanceOf(AuthorizationDeniedException.class);
		verify(service, never()).boards();
		verify(service, never()).post(any(), any(), any(), any());
		assertThat(controller.permissions(SecurityContextHolder.getContext().getAuthentication())
			.getResponse().manager()).isFalse();
	}

	@Test
	void roleContainingManagerKeywordInCurrentRoomCanUseCompetitionFeatures() {
		login("456", "ROLE_USER");
		givenRoles("456", List.of("섀버 대회 매니저"));

		controller.boards();

		verify(service).boards();
		assertThat(controller.permissions(SecurityContextHolder.getContext().getAuthentication())
			.getResponse().manager()).isTrue();
	}

	@Test
	void serverOwnerCanUseCompetitionFeaturesLikeAcademy() {
		login("789", "ROLE_ADMIN");

		controller.boards();

		verify(service).boards();
		verify(users, never()).findByBotIdAndDiscordID(any(), any());
	}

	private CustomUser login(String discordId, String role) {
		CustomUser user = CustomUser.of(discordId, "OAUTH_USER", role, null);
		SecurityContextHolder.getContext().setAuthentication(
			new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
		return user;
	}

	private void givenRoles(String discordId, List<String> roles) {
		when(users.findByBotIdAndDiscordID(BOT_ID, discordId))
			.thenReturn(Optional.of(DiscordUser.of(BOT_ID, discordId, roles)));
	}
}
