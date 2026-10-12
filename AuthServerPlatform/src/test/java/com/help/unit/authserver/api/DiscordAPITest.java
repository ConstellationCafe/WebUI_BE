package com.help.authserver.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.help.authserver.domain.user.dto.discord.DiscordGuildDto;
import com.help.authserver.domain.user.dto.discord.DiscordMeDto;
import com.help.authserver.domain.user.dto.user.DiscordUserDto;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class DiscordAPITest {

    private static final String TOKEN_URI = "https://discord.test/oauth2/token";
    private static final String USER_URI = "https://discord.test/users/@me";
    private static final String GUILDS_URI = "https://discord.test/users/@me/guilds";

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
        ReflectionTestUtils.setField(discordAPI, "userUri", USER_URI);
        ReflectionTestUtils.setField(discordAPI, "guildsUri", GUILDS_URI);
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

    @Test
    void profileFetchOnlyCallsDiscordMe() {
        server.expect(requestTo(USER_URI))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"id\":\"123\",\"username\":\"tester\",\"global_name\":\"Star\",\"avatar\":null}",
                        MediaType.APPLICATION_JSON
                ));

        DiscordMeDto me = discordAPI.getMe("discord-access-token");

        assertEquals("123", me.id());
        assertEquals("Star", me.globalName());
        server.verify();
    }

    @Test
    void guildFetchOnlyCallsDiscordGuilds() {
        server.expect(requestTo(GUILDS_URI + "?with_counts=true"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "[{\"id\":\"guild-1\",\"name\":\"Cafe\",\"icon\":null,\"approximate_member_count\":12}]",
                        MediaType.APPLICATION_JSON
                ));

        List<DiscordGuildDto> guilds = discordAPI.getGuilds("discord-access-token");

        assertEquals(List.of(new DiscordGuildDto("guild-1", "Cafe", null, 12)), guilds);
        server.verify();
    }

    @Test
    void loginProfileStillFetchesMeAndGuilds() {
        server.expect(requestTo(USER_URI))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"id\":\"123\",\"username\":\"tester\",\"global_name\":\"Star\",\"avatar\":null}",
                        MediaType.APPLICATION_JSON
                ));
        server.expect(requestTo(GUILDS_URI + "?with_counts=true"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        DiscordUserDto user = discordAPI.getUserInfo("discord-access-token");

        assertEquals("123", user.discordId());
        assertEquals("Star", user.globalName());
        assertEquals(List.of(), user.guilds());
        server.verify();
    }
}
