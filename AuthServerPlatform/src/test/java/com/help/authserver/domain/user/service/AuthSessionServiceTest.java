package com.help.authserver.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.help.authserver.domain.user.repository.SessionRepository;
import com.help.global.discord.config.ERPSubscriberRepository;
import com.help.global.discord.config.ErpSubscriber;
import com.help.global.jwt.CustomUser;
import com.help.global.jwt.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;

/**
 * 2026-09-26: selectRoom()이 방 선택 이전 권한(파라미터로 들어온 user) 대신,
 * botId 스코프로 재조회된 identity로 토큰을 발급하도록 바꾼 것을 검증한다.
 *
 * 이전에는 identityResolver.resolveIdentity(username, botId)의 반환값을
 * "멤버 여부 확인"에만 쓰고 버린 뒤, 방 선택 이전의 user로 토큰을 만들었다
 * — admin 여부가 방(botId) 단위로 재정의되는 이 시스템에서, refresh()를
 * 호출하기 전까지 잘못된 권한의 토큰이 발급되는 시간창이 있었다. refresh()와
 * 동일하게 재조회 결과를 그대로 쓰도록 통일했다.
 */
@ExtendWith(MockitoExtension.class)
class AuthSessionServiceTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private ERPSubscriberRepository erpSubscriberRepository;

    @Mock
    private IdentityResolver identityResolver;

    private AuthSessionService service;

    @BeforeEach
    void setUp() {
        service = new AuthSessionService(
                jwtUtil,
                sessionRepository,
                erpSubscriberRepository,
                identityResolver
        );
    }

    @Test
    void selectRoomIssuesTokenFromBotScopedIdentityNotThePreSelectionUser() {
        CustomUser preSelectionUser =
                CustomUser.of("123", "OAUTH_USER", "ROLE_USER", null);
        CustomUser scopedIdentity =
                CustomUser.of("123", "OAUTH_USER", "ROLE_ADMIN", null);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(erpSubscriberRepository.findByGuildId("guild-1"))
                .thenReturn(Optional.of(
                        new ErpSubscriber("bot-1", "guild-1", "123", null)
                ));
        when(identityResolver.resolveIdentity("123", "bot-1"))
                .thenReturn(Optional.of(scopedIdentity));
        when(jwtUtil.createAccessToken(any(CustomUser.class), eq("bot-1")))
                .thenReturn("access-token");
        when(jwtUtil.createRefreshToken(any(CustomUser.class), eq("bot-1")))
                .thenReturn("refresh-token");
        when(jwtUtil.createAccessTokenCookie("access-token"))
                .thenReturn(ResponseCookie.from("access_token", "access-token").build());
        when(jwtUtil.createRefreshTokenCookie("refresh-token"))
                .thenReturn(ResponseCookie.from("refresh_token", "refresh-token").build());

        service.selectRoom(preSelectionUser, "guild-1", response);

        ArgumentCaptor<CustomUser> accessTokenIdentity =
                ArgumentCaptor.forClass(CustomUser.class);
        verify(jwtUtil).createAccessToken(accessTokenIdentity.capture(), eq("bot-1"));

        ArgumentCaptor<CustomUser> refreshTokenIdentity =
                ArgumentCaptor.forClass(CustomUser.class);
        verify(jwtUtil).createRefreshToken(refreshTokenIdentity.capture(), eq("bot-1"));

        assertThat(accessTokenIdentity.getValue()).isSameAs(scopedIdentity);
        assertThat(refreshTokenIdentity.getValue()).isSameAs(scopedIdentity);
    }
}
