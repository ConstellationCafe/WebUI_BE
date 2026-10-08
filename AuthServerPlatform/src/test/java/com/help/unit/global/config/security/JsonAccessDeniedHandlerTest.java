package com.help.global.config.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

class JsonAccessDeniedHandlerTest {

    private final JsonAccessDeniedHandler handler = new JsonAccessDeniedHandler(new ObjectMapper());

    @Test
    void nonAdminOnAdminPathReceivesNotFoundJson() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/points/members");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, new AccessDeniedException("denied"));

        assertEquals(404, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        String body = response.getContentAsString();
        assertTrue(body.contains("\"success\":false"), body);
        assertTrue(body.contains("\"status\":404"), body);
    }
}
