package com.help.erpweb.domain.modules.competition.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.help.erpweb.domain.modules.competition.authorization.CompetitionAuthorization;
import com.help.erpweb.domain.modules.competition.dto.response.CompetitionBoardResponse;
import com.help.erpweb.domain.modules.competition.service.CompetitionService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

class CompetitionControllerTest {
	private CompetitionService service;
	private CompetitionAuthorization authorization;
	private CompetitionController controller;

	@BeforeEach
	void setUp() {
		service = mock(CompetitionService.class);
		authorization = mock(CompetitionAuthorization.class);
		controller = new CompetitionController(service, authorization);
	}

	@Test
	void permissionEndpointReturnsTheCurrentMemberManagerDecision() {
		Authentication authentication = mock(Authentication.class);
		when(authorization.isManager(authentication)).thenReturn(false);

		var response = controller.permissions(authentication);

		assertThat(response.isSuccess()).isTrue();
		assertThat(response.getResponse().manager()).isFalse();
		verify(authorization).isManager(authentication);
	}

	@Test
	void boardEndpointReturnsBoardsFromTheCompetitionService() {
		List<CompetitionBoardResponse> boards = List.of();
		when(service.boards()).thenReturn(boards);

		assertThat(controller.boards().getResponse()).isSameAs(boards);
		verify(service).boards();
	}
}
