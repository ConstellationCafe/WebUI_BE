package com.help.global.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.help.global.data.Authority;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class IntegrationApiKeyAuthenticationFilterTest {
    // 테스트 전용 값. 실제 키가 아니다.
    private static final String TEST_KEY = "test-only-integration-key";

    private final IntegrationApiKeyAuthenticationFilter filter = new IntegrationApiKeyAuthenticationFilter(
            new IntegrationClientProperties(List.of(
                    new IntegrationClientProperties.Client(
                            "discord-bot",
                            IntegrationApiKeyAuthenticationFilter.sha256Hex(TEST_KEY),
                            List.of("1001", " 1002 ")
                    )
            ))
    );

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private Authentication run(String apiKey) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/integrations/notifications");
        if (apiKey != null) {
            request.addHeader(IntegrationApiKeyAuthenticationFilter.HEADER, apiKey);
        }
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Test
    void validKeyAuthenticatesClientWithIntegrationRoleAndAllowedGuilds() throws Exception {
        Authentication authentication = run(TEST_KEY);

        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly(Authority.INTEGRATION);
        IntegrationClient client = (IntegrationClient) authentication.getPrincipal();
        assertThat(client.id()).isEqualTo("discord-bot");
        assertThat(client.canPublishTo("1002")).isTrue();
        assertThat(client.canPublishTo("9999")).isFalse();
    }

    @Test
    void wrongOrMissingKeyLeavesRequestUnauthenticated() throws Exception {
        assertThat(run("wrong-key")).isNull();
        SecurityContextHolder.clearContext();
        assertThat(run(null)).isNull();
    }

    @Test
    void principalDoesNotExposeKeyMaterial() throws Exception {
        assertThat(run(TEST_KEY).getPrincipal().toString()).doesNotContain(TEST_KEY);
    }

    @Test
    void misconfiguredClientFailsFastAtStartup() {
        assertThatThrownBy(() -> new IntegrationClientProperties.Client("bot", "not-a-hash", List.of("1")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new IntegrationClientProperties.Client(
                "bot",
                IntegrationApiKeyAuthenticationFilter.sha256Hex(TEST_KEY),
                List.of()
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void noConfiguredClientsMeansFeatureIsDisabled() throws Exception {
        IntegrationApiKeyAuthenticationFilter disabled =
                new IntegrationApiKeyAuthenticationFilter(new IntegrationClientProperties(null));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/integrations/notifications");
        request.addHeader(IntegrationApiKeyAuthenticationFilter.HEADER, TEST_KEY);

        disabled.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
