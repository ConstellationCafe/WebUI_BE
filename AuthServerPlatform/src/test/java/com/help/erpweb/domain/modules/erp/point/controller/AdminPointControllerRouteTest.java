package com.help.erpweb.domain.modules.erp.point.controller;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * ADR-0002: 관리자 API는 /api/admin/** 아래에 있어야 SecurityConfig의
 * ROLE_ADMIN URL 규칙을 받는다. 경로가 이 규칙에서 벗어나는 회귀를 막는다.
 */
class AdminPointControllerRouteTest {

    @Test
    void adminPointControllerIsMountedUnderAdminPrefixWithTemporaryLegacyPath() {
        RequestMapping mapping = AdminPointController.class.getAnnotation(RequestMapping.class);

        assertNotNull(mapping);
        assertArrayEquals(
                new String[] {"/api/admin/points", "/api/repository/membership/admin/points"},
                mapping.value()
        );
    }

    @Test
    void adminPointControllerKeepsMethodLevelAdminGuard() {
        // 호환 경로는 /api/admin/** URL 규칙 밖이므로 이 어노테이션이 유일한 관리자 검사다.
        assertNotNull(AdminPointController.class.getAnnotation(PreAuthorize.class));
    }
}
