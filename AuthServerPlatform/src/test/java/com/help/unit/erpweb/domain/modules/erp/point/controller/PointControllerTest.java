package com.help.erpweb.domain.modules.erp.point.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.help.erpweb.domain.modules.erp.point.service.PointService;
import com.help.global.common.response.ApiResponse;
import com.help.global.jwt.CustomUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PointControllerTest {
	private PointService pointService;
	private PointController controller;

	@BeforeEach
	void setUp() {
		pointService = mock(PointService.class);
		controller = new PointController(pointService);
	}

	@Test
	void personalPointHistoryUsesAuthenticatedPrincipalAndRequestedPage() {
		CustomUser user = mock(CustomUser.class);
		ApiResponse<?> expected = ApiResponse.success("history");
		when(pointService.getPointLog(user, 2, 50)).thenReturn(expected);

		assertThat(controller.getPointLog(user, 2, 50)).isSameAs(expected);
		verify(pointService).getPointLog(user, 2, 50);
	}
}
