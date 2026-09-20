package com.help.authserver.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class DiscordAPITest {

    private static final String TOKEN_URI = "https://discord.test/oauth2/token";

    private DiscordAPI discordAPI;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        discordAPI = new DiscordAPI(restTemplate);

        ReflectionTestUtils.setField(discordAPI, "clientId", "client-id");
        ReflectionTestUtils.setField(discordAPI, "clientSecret", "client-secret");
        ReflectionTestUtils.setField(discordAPI, "tokenUri", TOKEN_URI);
        ReflectionTestUtils.setField(discordAPI, "redirectUri", "https://frontend.test/callback");
    }

    @Test
    void returnsAccessTokenFromValidResponse() {
        server.expect(requestTo(TOKEN_URI))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"token-value\"}", MediaType.APPLICATION_JSON));

        assertEquals("token-value", discordAPI.exchangeCodeForToken("authorization-code"));
        server.verify();
    }

    @Test
    void rejectsResponseWithoutAccessToken() {
        server.expect(requestTo(TOKEN_URI))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThrows(
                IllegalStateException.class,
                () -> discordAPI.exchangeCodeForToken("authorization-code")
        );
        server.verify();
    }
}
