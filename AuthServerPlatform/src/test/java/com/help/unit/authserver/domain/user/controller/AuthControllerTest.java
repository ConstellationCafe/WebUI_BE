package com.help.authserver.domain.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.help.authserver.domain.user.service.AuthSessionService;
import com.help.global.common.response.ApiResponse;
import com.help.global.jwt.CustomUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthControllerTest {
	private AuthSessionService authSessionService;
	private AuthController controller;

	@BeforeEach
	void setUp() {
		authSessionService = mock(AuthSessionService.class);
		controller = new AuthController(authSessionService);
	}

	@Test
	void refreshDelegatesRequestAndResponseToSessionService() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		ApiResponse<?> expected = ApiResponse.success("refreshed");
		when(authSessionService.refresh(request, response)).thenReturn(expected);

		assertThat(controller.refresh(request, response)).isSameAs(expected);
		verify(authSessionService).refresh(request, response);
	}

	@Test
	void loginCheckDelegatesTheRequestToSessionService() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		ApiResponse<?> expected = ApiResponse.success(true);
		when(authSessionService.checkLogin(request)).thenReturn(expected);

		assertThat(controller.loginCheck(request)).isSameAs(expected);
		verify(authSessionService).checkLogin(request);
	}

	@Test
	void logoutClearsTheAuthenticatedSessionAndReturnsEmptySuccess() {
		CustomUser user = mock(CustomUser.class);
		HttpServletResponse response = mock(HttpServletResponse.class);

		ApiResponse<?> result = controller.logout(user, response);

		assertThat(result.isSuccess()).isTrue();
		assertThat(result.getResponse()).isNull();
		verify(authSessionService).logout(user, response);
	}
}
