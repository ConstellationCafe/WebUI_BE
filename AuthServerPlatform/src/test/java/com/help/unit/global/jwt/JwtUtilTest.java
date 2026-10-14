package com.help.global.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import com.help.global.discord.identity.DiscordUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
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
                discordUser.getDiscordId(),
                discordUser.getPassword(),
                discordUser.getRoleName()
        );
    }

    @Test
    void accessTokenRoundTripsUsernameAndAuthentication() {
        String token = jwtUtil.createAccessToken(user);

        assertTrue(jwtUtil.isTokenValidate(token));
        assertEquals("discord-user", jwtUtil.extractDiscordId(token).orElseThrow());
    }

    @Test
    void accessTokenCarriesDiscordIdClaimWithoutLegacyClaims() {
        Claims claims = parse(jwtUtil.createAccessToken(user, "bot-1"));

        assertEquals("discord-user", claims.get("discordId", String.class));
        assertEquals("bot-1", claims.get("botId", String.class));
        assertFalse(claims.containsKey("username"));
        assertFalse(claims.containsKey("nickname"));
    }

    @Test
    void extractsDiscordIdFromLegacyUsernameClaim() {
        // ADR-0008 이전에 발급된 RefreshToken은 username claim에 Discord ID를 담고 있다.
        String legacyToken = Jwts.builder()
                .claim("username", "legacy-user")
                .subject("RefreshToken")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(signingKey())
                .compact();

        assertTrue(jwtUtil.isTokenValidate(legacyToken));
        assertEquals("legacy-user", jwtUtil.extractDiscordId(legacyToken).orElseThrow());
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtUtil.createAccessToken(user);

        assertFalse(jwtUtil.isTokenValidate(token + "tampered"));
        assertTrue(jwtUtil.extractDiscordId(token + "tampered").isEmpty());
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

    private static SecretKey signingKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
    }

    private static Claims parse(String token) {
        return Jwts.parser().verifyWith(signingKey()).build().parseSignedClaims(token).getPayload();
    }
}
