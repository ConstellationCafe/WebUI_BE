package com.help.erpweb.domain.modules.erp.penalty.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * ADR-0006: 벌점 관리 API는 운영 역할도 쓰므로 /api/penalties에 두고 @penaltyAuth로 확인한다.
 * 호환 경로 /api/admin/penalties는 ADR-0002 URL 규칙으로 서버장만 통과한다.
 */
class PenaltyControllerRouteTest {
	@Test
	void penaltyControllerIsMountedOnNewPathAndDeprecatedAdminPath() {
		RequestMapping mapping = PenaltyController.class.getAnnotation(RequestMapping.class);

		assertThat(mapping.value()).containsExactly("/api/penalties", "/api/admin/penalties");
	}

	@Test
	void penaltyControllerKeepsMethodLevelGuard() {
		PreAuthorize guard = PenaltyController.class.getAnnotation(PreAuthorize.class);

		assertThat(guard.value()).isEqualTo("@penaltyAuth.isManager(authentication)");
	}
}
