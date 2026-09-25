package com.help.global.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.help.global.discord.constellation.DiscordUser;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

class JwtUtilTest {

    private static final String SECRET =
            "VGhpc0lzQVN1ZmZpY2llbnRseUxvbmdTZWNyZXRLZXlGb3JKV1Q=";

    private JwtUtil jwtUtil;
    private CustomUser user;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secretKeyBase64", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "activeProfiles", "dev");
        jwtUtil.init();

        DiscordUser discordUser =
                DiscordUser.of("test-bot", "discord-user", List.of("서버장"));
        user = CustomUser.of(
                discordUser.getUsername(),
                discordUser.getPassword(),
                discordUser.getRoleName(),
                null
        );
    }

    @Test
    void accessTokenRoundTripsUsernameAndAuthentication() {
        String token = jwtUtil.createAccessToken(user);

        assertTrue(jwtUtil.isTokenValidate(token));
        assertEquals("discord-user", jwtUtil.extractUsername(token).orElseThrow());
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtUtil.createAccessToken(user);

        assertFalse(jwtUtil.isTokenValidate(token + "tampered"));
        assertTrue(jwtUtil.extractUsername(token + "tampered").isEmpty());
    }

    @Test
    void accessTokenCookieIsHttpOnlyAndDevCookieIsNotSecure() {
        ResponseCookie cookie = jwtUtil.createAccessTokenCookie("token");

        assertTrue(cookie.isHttpOnly());
        assertFalse(cookie.isSecure());
        assertEquals("/", cookie.getPath());
    }

    @Test
    void extractsOnlyAccessTokenCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(
                new Cookie("RefreshToken", "refresh"),
                new Cookie("AccessToken", "access")
        );

        assertEquals(
                "access",
                jwtUtil.extractAccessTokenFromRequest(request).orElseThrow()
        );
        assertEquals(
                "refresh",
                jwtUtil.extractRefreshTokenFromRequest(request).orElseThrow()
        );
    }
}
