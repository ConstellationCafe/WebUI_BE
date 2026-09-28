package com.help.erpweb.domain.modules.erp.penalty.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.help.erpweb.domain.modules.erp.penalty.dto.request.PenaltySort;
import com.help.erpweb.domain.modules.erp.penalty.service.PenaltyService;
import com.help.global.jwt.CustomUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

class PenaltyAuthorizationTest {
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void nonAdminIsRejectedBeforeServiceRuns() {
        try (var context = new AnnotationConfigApplicationContext(PenaltyAuthorizationConfig.class)) {
            CustomUser user = CustomUser.of("123", "OAUTH_USER", "ROLE_USER", null);
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));

            assertThatThrownBy(() -> context.getBean(AdminPenaltyController.class)
                    .history(null, null, PenaltySort.OCCURRED_AT_DESC, 1, 20))
                    .isInstanceOf(AuthorizationDeniedException.class);
            verify(context.getBean(PenaltyService.class), never())
                    .history(null, null, PenaltySort.OCCURRED_AT_DESC, 1, 20);
        }
    }
}
