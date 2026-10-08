package com.help.authserver.domain.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.help.authserver.api.DiscordAPI;
import com.help.authserver.domain.user.dto.discord.DiscordGuildDto;
import com.help.authserver.domain.user.dto.discord.DiscordMeDto;
import com.help.authserver.domain.user.dto.user.CurrentUserDto;
import com.help.authserver.domain.user.entity.SessionInfo;
import com.help.authserver.domain.user.repository.SessionRepository;
import com.help.global.common.response.ApiResponse;
import com.help.global.discord.config.ERPSubscriberRepository;
import com.help.global.discord.config.ErpSubscriber;
import com.help.global.discord.identity.DiscordUserRepository;
import com.help.global.jwt.CustomUser;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;

class DiscordLoginServiceTest {
    private static final String DISCORD_ID = "123";
    private static final String ACCESS_TOKEN = "test-token";

    private final DiscordAPI discordAPI = mock(DiscordAPI.class);
    private final DiscordUserRepository userRepository = mock(DiscordUserRepository.class);
    private final SessionRepository sessionRepository = mock(SessionRepository.class);
    private final ERPSubscriberRepository erpSubscriberRepository = mock(ERPSubscriberRepository.class);
    private final AuthSessionService authSessionService = mock(AuthSessionService.class);

    private DiscordLoginService service;
    private CustomUser user;

    @BeforeEach
    void setUp() {
        service = new DiscordLoginService(
                discordAPI, userRepository, sessionRepository,
                erpSubscriberRepository, authSessionService
        );
        user = CustomUser.of(DISCORD_ID, "OAUTH_USER", "ROLE_ADMIN", null);
        when(sessionRepository.find(DISCORD_ID))
                .thenReturn(Optional.of(new SessionInfo(DISCORD_ID, true, ACCESS_TOKEN, 0)));
    }

    @Test
    void meUsesOnlyProfileAndKeepsRoomAuthorities() {
        when(discordAPI.getMe(ACCESS_TOKEN))
                .thenReturn(new DiscordMeDto(DISCORD_ID, "tester", "Star", "avatar"));

        ApiResponse<?> response = service.me(user);

        assertTrue(response.isSuccess());
        assertEquals(
                new CurrentUserDto(DISCORD_ID, "tester", "Star", "avatar", List.of("ROLE_ADMIN")),
                response.getResponse()
        );
        verify(discordAPI, never()).getGuilds(ACCESS_TOKEN);
    }

    @Test
    void guildsUseOnlyGuildEndpointAndFilterRegisteredRooms() {
        DiscordGuildDto registered = new DiscordGuildDto("guild-1", "Cafe", null, 12);
        DiscordGuildDto unregistered = new DiscordGuildDto("guild-2", "Other", null, 8);
        when(discordAPI.getGuilds(ACCESS_TOKEN))
                .thenReturn(List.of(registered, unregistered));
        when(erpSubscriberRepository.findByGuildIdIn(List.of("guild-1", "guild-2")))
                .thenReturn(List.of(new ErpSubscriber("bot-1", "guild-1", DISCORD_ID, null)));

        ApiResponse<?> response = service.guilds(user);

        assertTrue(response.isSuccess());
        assertEquals(List.of(registered), response.getResponse());
        verify(discordAPI, never()).getMe(ACCESS_TOKEN);
    }

    @Test
    void guildsKeepStoredGuildFallbackOnDiscordFailure() {
        when(discordAPI.getGuilds(ACCESS_TOKEN))
                .thenThrow(new RestClientException("Discord unavailable"));
        when(erpSubscriberRepository.findByDiscordId(DISCORD_ID))
                .thenReturn(List.of(new ErpSubscriber("bot-1", "guild-1", DISCORD_ID, null)));

        ApiResponse<?> response = service.guilds(user);

        assertEquals(List.of(new DiscordGuildDto("guild-1", null, null, 0)), response.getResponse());
        verify(erpSubscriberRepository).findByDiscordId(DISCORD_ID);
    }
}
