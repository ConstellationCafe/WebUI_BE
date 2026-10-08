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
    void adminPointControllerIsMountedOnlyUnderAdminPrefix() {
        RequestMapping mapping = AdminPointController.class.getAnnotation(RequestMapping.class);

        assertNotNull(mapping);
        assertArrayEquals(
                new String[] {"/api/admin/points"},
                mapping.value()
        );
    }

    @Test
    void adminPointControllerKeepsMethodLevelAdminGuard() {
        // SecurityConfig URL 규칙과 함께 이중으로 관리자 권한을 확인한다.
        assertNotNull(AdminPointController.class.getAnnotation(PreAuthorize.class));
    }
}
