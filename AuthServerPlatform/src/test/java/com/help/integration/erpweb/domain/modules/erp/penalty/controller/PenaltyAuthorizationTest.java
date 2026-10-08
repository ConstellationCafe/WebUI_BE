package com.help.erpweb.domain.modules.erp.penalty.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
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

import com.help.erpweb.domain.modules.erp.penalty.dto.request.PenaltyCancelRequest;
import com.help.erpweb.domain.modules.erp.penalty.dto.request.PenaltySort;
import com.help.erpweb.domain.modules.erp.penalty.service.PenaltyService;
import com.help.global.discord.identity.DiscordUser;
import com.help.global.discord.identity.DiscordUserRepository;
import com.help.global.guild.GuildContext;
import com.help.global.jwt.CustomUser;

/** ADR-0006: 운영 매니저·운영 본부원·서버장만 벌점을 관리한다. */
class PenaltyAuthorizationTest {
	private static final String BOT_ID = "bot-a";
	private AnnotationConfigApplicationContext context;
	private PenaltyController controller;
	private PenaltyPermissionController permissionController;
	private PenaltyService service;
	private DiscordUserRepository users;

	@BeforeEach
	void setUp() {
		context = new AnnotationConfigApplicationContext(PenaltyAuthorizationConfig.class);
		controller = context.getBean(PenaltyController.class);
		permissionController = context.getBean(PenaltyPermissionController.class);
		service = context.getBean(PenaltyService.class);
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
	void memberWithoutOperationRoleIsRejectedBeforeServiceRuns() {
		CustomUser user = login("123", "ROLE_USER");
		givenRoles("123", List.of("본부원", "대회 매니저", "운영"));

		assertThatThrownBy(() -> controller.history(null, null, PenaltySort.OCCURRED_AT_DESC, 1, 20))
			.isInstanceOf(AuthorizationDeniedException.class);
		assertThatThrownBy(() -> controller.cancel(1L, new PenaltyCancelRequest("오부여"), user))
			.isInstanceOf(AuthorizationDeniedException.class);
		verify(service, never()).history(any(), any(), any(), anyInt(), anyInt());
		verify(service, never()).cancel(anyLong(), any(), any());
		assertThat(permissionsOfCurrentUser()).isFalse();
	}

	@Test
	void memberNotInCurrentRoomIsRejected() {
		login("404", "ROLE_USER");
		when(users.findByBotIdAndDiscordID(BOT_ID, "404")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> controller.ranking(null, 1, 20))
			.isInstanceOf(AuthorizationDeniedException.class);
		assertThat(permissionsOfCurrentUser()).isFalse();
	}

	@Test
	void operationManagerRoleCanManagePenalties() {
		login("456", "ROLE_USER");
		givenRoles("456", List.of("섀버 운영 매니저"));

		controller.history(null, null, PenaltySort.OCCURRED_AT_DESC, 1, 20);

		verify(service).history(null, null, PenaltySort.OCCURRED_AT_DESC, 1, 20);
		assertThat(permissionsOfCurrentUser()).isTrue();
	}

	@Test
	void operationHeadquartersMemberRoleCanManagePenalties() {
		login("457", "ROLE_USER");
		givenRoles("457", List.of("운영 본부원"));

		controller.ranking(null, 1, 20);

		verify(service).ranking(null, 1, 20);
		assertThat(permissionsOfCurrentUser()).isTrue();
	}

	@Test
	void serverOwnerCanManagePenaltiesWithoutRoleLookup() {
		login("789", "ROLE_ADMIN");

		controller.ranking(null, 1, 20);

		verify(service).ranking(null, 1, 20);
		verify(users, never()).findByBotIdAndDiscordID(any(), any());
		assertThat(permissionsOfCurrentUser()).isTrue();
	}

	private boolean permissionsOfCurrentUser() {
		return permissionController.permissions(SecurityContextHolder.getContext().getAuthentication())
			.getResponse().manager();
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
