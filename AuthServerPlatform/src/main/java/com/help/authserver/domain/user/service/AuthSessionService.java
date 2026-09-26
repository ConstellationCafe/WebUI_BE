package com.help.authserver.domain.user.service;

import com.help.authserver.domain.user.dto.response.LoginCheckResponseDto;
import com.help.authserver.domain.user.entity.SessionInfo;
import com.help.authserver.domain.user.repository.SessionRepository;
import com.help.global.common.exception.CustomException;
import com.help.global.common.exception.ErrorCode;
import com.help.global.common.response.ApiResponse;
import com.help.global.discord.config.ERPSubscriberRepository;
import com.help.global.discord.config.ErpSubscriber;
import com.help.global.jwt.CustomUser;
import com.help.global.jwt.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * JWT 발급/검증, Redis 세션, 쿠키 관리 — 로그인 이후(그리고 로그인 마지막
 * 단계)의 프로바이더 무관 로직을 전담한다.
 *
 * "누가(어떤 OAuth 프로바이더로) 로그인했는지"는 여기서 모른다. 프로바이더별
 * {@link OAuthLoginService} 구현체가 자기 쪽 code 교환/프로필 조회를 끝내고
 * {@link CustomUser}를 만든 뒤 이 서비스에 위임하는 구조다. 로그인 완료
 * 이후 신원/역할을 다시 조회해야 하는 지점({@link #refresh}, {@link
 * #selectRoom})은 {@link IdentityResolver}를 통해서만 접근하므로, 여기 역시
 * 어떤 프로바이더의 저장소를 쓰는지 모른다.
 *
 * 주의: {@link #selectRoom}의 guildId -> botId 매핑은 ERPSubscriberRepository를
 * 그대로 쓴다. 이 테이블은 이미 com.help.global 아래에 있어 프로바이더 전용은
 * 아니지만, 컬럼명(guildId)은 Discord 어휘다 — 향후 두 번째 프로바이더가
 * "방" 개념을 다르게 표현한다면 이 부분은 별도 추상화가 필요하다.
 */
@Service
@RequiredArgsConstructor
public class AuthSessionService {

    private final JwtUtil jwtUtil;
    private final SessionRepository sessionRepository;
    private final ERPSubscriberRepository erpSubscriberRepository;
    private final IdentityResolver identityResolver;

    /**
     * 로그인 마지막 단계(ADR-0001 1단계: discordId 인증만 끝난 상태). botId
     * 없는 AccessToken/RefreshToken을 발급하고 Redis 세션을 새로 만든다.
     * 프로바이더별 {@link OAuthLoginService} 구현체가 자기 쪽 로그인 처리를
     * 끝낸 뒤 여기로 위임한다.
     */
    public void issueSession(
            final CustomUser identity,
            final String providerAccessToken,
            final HttpServletResponse response
    ) {
        final String accessToken = jwtUtil.createAccessToken(identity);
        final String refreshToken = jwtUtil.createRefreshToken(identity);

        final SessionInfo sessionInfo = new SessionInfo(
                identity.getUsername(),
                true,
                providerAccessToken,
                System.currentTimeMillis()
        );

        sessionRepository.save(sessionInfo, jwtUtil.getRefreshTokenTtl());

        response.addHeader(
                "Set-Cookie",
                jwtUtil.createAccessTokenCookie(accessToken).toString()
        );
        response.addHeader(
                "Set-Cookie",
                jwtUtil.createRefreshTokenCookie(refreshToken).toString()
        );
    }

    public ApiResponse<?> checkLogin(final HttpServletRequest request) {
        final Optional<String> validAccessToken =
                jwtUtil.extractAccessTokenFromRequest(request)
                        .filter(jwtUtil::isTokenValidate);

        final boolean isLogin = validAccessToken.isPresent();

        final boolean refreshHint =
                jwtUtil.extractRefreshTokenFromRequest(request)
                        .filter(jwtUtil::isTokenValidate)
                        .isPresent();

        final boolean roomSelected =
                validAccessToken
                        .flatMap(jwtUtil::extractBotId)
                        .isPresent();

        return ApiResponse.success(
                new LoginCheckResponseDto(isLogin, refreshHint, roomSelected)
        );
    }

    public ApiResponse<?> refresh(
            final HttpServletRequest request,
            final HttpServletResponse response
    ) {
        final String refreshToken =
                jwtUtil.extractRefreshTokenFromRequest(request)
                        .filter(jwtUtil::isTokenValidate)
                        .orElseThrow(() -> new CustomException(ErrorCode.INVALID_TOKEN));

        // ADR-0001: RefreshToken이 botId를 갖고 있으면(=채팅방을 선택한 뒤
        // 재발급하는 경우) 새 AccessToken에도 그대로 실어준다. 없으면 여전히
        // "선택 대기" 상태이므로 botId 없이 재발급한다.
        final String botId = jwtUtil.extractBotId(refreshToken).orElse(null);

        final String username =
                jwtUtil.extractUsername(refreshToken)
                        .orElseThrow(() -> new CustomException(ErrorCode.INVALID_TOKEN));

        final CustomUser identity =
                identityResolver.resolveIdentity(username, botId)
                        .orElseThrow(() -> new CustomException(ErrorCode.INVALID_TOKEN));

        final SessionInfo sessionInfo =
                sessionRepository.find(username)
                        .orElseThrow(() -> new CustomException(ErrorCode.SESSION_EXPIRED));

        if (!sessionInfo.isValid()) {
            throw new CustomException(ErrorCode.SESSION_REVOKED);
        }

        final String accessToken = jwtUtil.createAccessToken(identity, botId);

        final ResponseCookie accessCookie = jwtUtil.createAccessTokenCookie(accessToken);
        response.addHeader("Set-Cookie", accessCookie.toString());

        return ApiResponse.success(true);
    }

    /**
     * ADR-0001: discordId 인증만으로는 로그인이 완료되지 않는다. 이 메서드가
     * 호출되어야("채팅방 선택") botId가 담긴 AccessToken/RefreshToken이
     * 발급되고, 그 순간부터 /api/**가 이 사용자를 인증된 것으로 취급한다
     * (BackEndJwtAuthFilter 참고).
     *
     * roles(authorities)는 기존 동작 그대로 selectRoom 이전에 확인된 값을
     * 그대로 쓴다(재조회하지 않는다) — 이건 이번 리팩토링 이전부터 있던
     * 동작이라 그대로 옮겼다. refresh()는 반대로 botId 스코프로 재조회하므로
     * 둘의 동작이 다르다는 점은 별도로 보고할 사항이다.
     */
    public ApiResponse<?> selectRoom(
            final CustomUser user,
            final String guildId,
            final HttpServletResponse response
    ) {
        final String botId =
                erpSubscriberRepository.findByGuildId(guildId)
                        .map(ErpSubscriber::getBotId)
                        .orElseThrow(() -> new CustomException(ErrorCode.GUILD_NOT_REGISTERED));

        // ADR-0001: guildId가 "등록"되어 있다는 것만으로는 부족하다 — 이
        // discordId가 실제로 그 방(botId)의 멤버인지까지 검증해야 한다.
        identityResolver.resolveIdentity(user.getUsername(), botId)
                .orElseThrow(() -> new CustomException(ErrorCode.GUILD_MEMBER_NOT_FOUND));

        final String accessToken = jwtUtil.createAccessToken(user, botId);
        final String refreshToken = jwtUtil.createRefreshToken(user, botId);

        response.addHeader(
                "Set-Cookie",
                jwtUtil.createAccessTokenCookie(accessToken).toString()
        );
        response.addHeader(
                "Set-Cookie",
                jwtUtil.createRefreshTokenCookie(refreshToken).toString()
        );

        return ApiResponse.success(true);
    }

    public void logout(final CustomUser user, final HttpServletResponse response) {
        sessionRepository.invalidate(user.getUsername());

        response.addHeader(
                "Set-Cookie",
                jwtUtil.createExpiredAccessTokenCookie().toString()
        );
        response.addHeader(
                "Set-Cookie",
                jwtUtil.createExpiredRefreshTokenCookie().toString()
        );
    }
}
