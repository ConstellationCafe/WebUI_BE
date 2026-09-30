package com.help.erpweb.domain.modules.competition.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

import com.help.erpweb.domain.modules.competition.dto.request.CompetitionCreateRequest;
import com.help.erpweb.domain.modules.competition.service.CompetitionService;
import com.help.global.jwt.CustomUser;

class CompetitionAuthorizationTest {
	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void nonAdminCannotListBoardsOrPostBeforeServiceRuns() {
		try (var context = new AnnotationConfigApplicationContext(CompetitionAuthorizationConfig.class)) {
			CustomUser user = CustomUser.of("123", "OAUTH_USER", "ROLE_USER", null);
			SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
			AdminCompetitionController controller = context.getBean(AdminCompetitionController.class);
			CompetitionService service = context.getBean(CompetitionService.class);

			assertThatThrownBy(controller::boards).isInstanceOf(AuthorizationDeniedException.class);
			assertThatThrownBy(() -> controller.post(
				new CompetitionCreateRequest("req-1", "inner_board", null), user))
				.isInstanceOf(AuthorizationDeniedException.class);
			verify(service, never()).boards();
			verify(service, never()).post(any(), any(), any(), any());
		}
	}
}
