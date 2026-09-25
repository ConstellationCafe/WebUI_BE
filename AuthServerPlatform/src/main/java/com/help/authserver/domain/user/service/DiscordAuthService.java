package com.help.authserver.domain.user.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.help.authserver.api.LoginAPI;
import com.help.authserver.domain.user.dto.discord.DiscordGuildDto;
import com.help.authserver.domain.user.dto.response.LoginCheckResponseDto;
import com.help.authserver.domain.user.dto.user.CurrentUserDto;
import com.help.authserver.domain.user.dto.user.DiscordUserDto;
import com.help.authserver.domain.user.entity.SessionInfo;
import com.help.authserver.domain.user.entity.config.ErpSubscriber;
import com.help.authserver.domain.user.entity.constellation.DiscordUser;
import com.help.authserver.domain.user.repository.SessionRepository;
import com.help.authserver.domain.user.repository.config.ERPSubscriberRepository;
import com.help.authserver.domain.user.repository.constellation.DiscordUserRepository;
import com.help.global.common.exception.CustomException;
import com.help.global.common.exception.ErrorCode;
import com.help.global.common.response.ApiResponse;
import com.help.global.jwt.CustomUser;
import com.help.global.jwt.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class DiscordAuthService implements UserDetailsService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final JwtUtil jwtUtil;
    private final LoginAPI<DiscordUserDto> loginAPI;
    private final DiscordUserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final ERPSubscriberRepository erpSubscriberRepository;

    @Value("${front.redirect-uri}")
    private String redirectUri;

    @Value("${register-uri}")
    private String registerUri;

    @Override
    public UserDetails loadUserByUsername(
            final String discordID
    ) throws UsernameNotFoundException {

        // ADR-0001: 채팅방 선택 이전이라 botId 스코프가 없다 — 신원 확인만 수행한다.
        return userRepository.findIdentityByDiscordID(discordID)
                .map(CustomUser::from)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "UserDetails creation failed. discordID="
                                        + discordID
                        )
                );
    }

    public String login(
            final String code,
            final HttpServletResponse response
    ) {
        try {
            String accessTokenForDiscord =
                    loginAPI.exchangeCodeForToken(code);

            DiscordUserDto userDto =
                    loginAPI.getUserInfo(accessTokenForDiscord);

            UserDetails discordUser =
                    loadUserByUsername(userDto.discordId());

            String accessToken =
                    jwtUtil.createAccessToken(
                            (CustomUser) discordUser
                    );

            String refreshToken =
                    jwtUtil.createRefreshToken(
                            (CustomUser) discordUser
                    );

            SessionInfo sessionInfo = new SessionInfo(
                    discordUser.getUsername(),
                    true,
                    accessTokenForDiscord,
                    System.currentTimeMillis()
            );

            sessionRepository.save(
                    sessionInfo,
                    jwtUtil.getRefreshTokenTtl()
            );

            response.addHeader(
                    "Set-Cookie",
                    jwtUtil.createAccessTokenCookie(
                            accessToken
                    ).toString()
            );

            response.addHeader(
                    "Set-Cookie",
                    jwtUtil.createRefreshTokenCookie(
                            refreshToken
                    ).toString()
            );

            return redirectUri;

        } catch (UsernameNotFoundException e) {
            return registerUri;
        }
    }

    public ApiResponse<?> me(CustomUser user) {
        String username = user.getUsername();

        SessionInfo sessionInfo =
                sessionRepository.find(username)
                        .orElseThrow(() ->
                                new CustomException(
                                        ErrorCode.SESSION_EXPIRED
                                )
                        );

        if (!sessionInfo.isValid()) {
            throw new CustomException(ErrorCode.SESSION_REVOKED);
        }

        String discordAccessToken =
                sessionInfo.getDiscordAccessToken();

        if (discordAccessToken == null) {
            throw new CustomException(ErrorCode.SESSION_EXPIRED);
        }

        DiscordUserDto userDto =
                getUserInfo(
                        discordAccessToken
                );

        List<String> roles =
                user.getAuthorities()
                        .stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList();

        CurrentUserDto meDto = new CurrentUserDto(
                userDto.discordId(),
                userDto.username(),
                userDto.globalName(),
                userDto.avatar(),
                roles
        );

        return ApiResponse.success(meDto);
    }

    public ApiResponse<?> guilds(CustomUser user) {
        String username = user.getUsername();

        SessionInfo sessionInfo =
                sessionRepository.find(username)
                        .orElseThrow(() ->
                                new CustomException(
                                        ErrorCode.SESSION_EXPIRED
                                )
                        );

        String discordAccessToken =
                sessionInfo.getDiscordAccessToken();

        try {
            DiscordUserDto userDto =
                    getUserInfo(
                            discordAccessToken
                    );

            List<String> registeredGuildIds =
                    erpSubscriberRepository
                            .findByGuildIdIn(
                                    userDto.guilds()
                                            .stream()
                                            .map(DiscordGuildDto::id)
                                            .toList()
                            )
                            .stream()
                            .map(ErpSubscriber::getGuildId)
                            .toList();

            List<DiscordGuildDto> guilds =
                    userDto.guilds()
                            .stream()
                            .filter(guild ->
                                    registeredGuildIds.contains(
                                            guild.id()
                                    )
                            )
                            .toList();

            return ApiResponse.success(guilds);

        } catch (Exception e) {
            log.error("Discord guild 조회 실패. 저장된 guild 목록으로 대체합니다", e);

            List<DiscordGuildDto> guilds =
                    erpSubscriberRepository
                            .findByDiscordId(username)
                            .stream()
                            .map(subscriber ->
                                    new DiscordGuildDto(
                                            subscriber.getGuildId(),
                                            null,
                                            null,
                                            0
                                    )
                            )
                            .toList();

            return ApiResponse.success(guilds);
        }
    }

    private DiscordUserDto getUserInfo(
            String discordAccessToken
    ) {
        final int maxRetries = 1;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return loginAPI.getUserInfo(discordAccessToken);
            } catch (HttpClientErrorException.TooManyRequests exception) {
                if (attempt == maxRetries) {
                    throw new CustomException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
                }
                sleep(resolveRetryAfter(exception));
            } catch (RestClientException exception) {
                throw new CustomException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
            }
        }
        throw new CustomException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
    }

    private long resolveRetryAfter(HttpClientErrorException.TooManyRequests exception) {
        String retryAfter = exception.getResponseHeaders() == null
                ? null
                : exception.getResponseHeaders().getFirst("Retry-After");
        try {
            return Math.min(Math.max(Long.parseLong(retryAfter), 1L), 5L);
        } catch (Exception ignored) {
            try {
                JsonNode body = OBJECT_MAPPER.readTree(exception.getResponseBodyAsString());
                return Math.min(Math.max((long) Math.ceil(body.path("retry_after").asDouble(1.0)), 1L), 5L);
            } catch (Exception parseException) {
                return 1L;
            }
        }
    }

    private void sleep(long seconds) {
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new CustomException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
        }
    }

    public ApiResponse<?> checkLogin(
            final HttpServletRequest request
    ) {
        Optional<String> validAccessToken =
                jwtUtil.extractAccessTokenFromRequest(request)
                        .filter(jwtUtil::isTokenValidate);

        boolean isLogin = validAccessToken.isPresent();

        boolean refreshHint =
                jwtUtil.extractRefreshTokenFromRequest(request)
                        .filter(jwtUtil::isTokenValidate)
                        .isPresent();

        boolean roomSelected =
                validAccessToken
                        .flatMap(jwtUtil::extractBotId)
                        .isPresent();

        return ApiResponse.success(
                new LoginCheckResponseDto(
                        isLogin,
                        refreshHint,
                        roomSelected
                )
        );
    }

    public ApiResponse<?> refresh(
            final HttpServletRequest request,
            final HttpServletResponse response
    ) {
        final String refreshToken =
                jwtUtil.extractRefreshTokenFromRequest(request)
                        .filter(jwtUtil::isTokenValidate)
                        .orElseThrow(() ->
                                new CustomException(
                                        ErrorCode.INVALID_TOKEN
                                )
                        );

        // ADR-0001: RefreshToken이 botId를 갖고 있으면(=채팅방을 선택한 뒤
        // 재발급하는 경우) 새 AccessToken에도 그대로 실어준다. 없으면
        // 여전히 "선택 대기" 상태이므로 botId 없이 재발급한다.
        final String botId =
                jwtUtil.extractBotId(refreshToken)
                        .orElse(null);

        final String username =
                jwtUtil.extractUsername(refreshToken)
                        .orElseThrow(() ->
                                new CustomException(
                                        ErrorCode.INVALID_TOKEN
                                )
                        );

        // admin 여부(roles)가 방(botId) 단위로 갈리므로, 선택된 방이 있으면
        // 반드시 그 방으로 스코프해서 조회한다. 스코프 없이 조회하면 임의의
        // 방(다른 방)의 roles를 CustomUser에 실어버릴 수 있다.
        final DiscordUser user =
                (botId != null
                        ? userRepository.findByBotIdAndDiscordID(botId, username)
                        : userRepository.findIdentityByDiscordID(username))
                        .orElseThrow(() ->
                                new CustomException(
                                        ErrorCode.INVALID_TOKEN
                                )
                        );

        SessionInfo sessionInfo =
                sessionRepository.find(username)
                        .orElseThrow(() ->
                                new CustomException(
                                        ErrorCode.SESSION_EXPIRED
                                )
                        );

        if (!sessionInfo.isValid()) {
            throw new CustomException(
                    ErrorCode.SESSION_REVOKED
            );
        }

        final String accessToken =
                jwtUtil.createAccessToken(
                        CustomUser.from(user),
                        botId
                );

        ResponseCookie accessCookie =
                jwtUtil.createAccessTokenCookie(
                        accessToken
                );

        response.addHeader(
                "Set-Cookie",
                accessCookie.toString()
        );

        return ApiResponse.success(true);
    }

    /**
     * ADR-0001: discordId 인증만으로는 로그인이 완료되지 않는다.
     * 이 메서드가 호출되어야("채팅방 선택") botId가 담긴 AccessToken/
     * RefreshToken이 발급되고, 그 순간부터 /api/**가 이 사용자를
     * 인증된 것으로 취급한다(BackEndJwtAuthFilter 참고).
     */
    public ApiResponse<?> selectGuild(
            final CustomUser user,
            final String guildId,
            final HttpServletResponse response
    ) {
        final String botId =
                erpSubscriberRepository
                        .findByGuildId(guildId)
                        .map(ErpSubscriber::getBotId)
                        .orElseThrow(() ->
                                new CustomException(
                                        ErrorCode.GUILD_NOT_REGISTERED
                                )
                        );

        // ADR-0001: guildId가 "등록"되어 있다는 것만으로는 부족하다 — 이
        // discordId가 실제로 그 방(botId)의 멤버인지까지 검증해야 한다.
        // 검증하지 않으면 admin 여부가 방 단위로 갈리더라도, 등록된 다른
        // 방을 골라 그 방의 멤버가 아닌 채로 로그인을 완료해버릴 수 있다.
        userRepository.findByBotIdAndDiscordID(botId, user.getUsername())
                .orElseThrow(() -> new CustomException(ErrorCode.GUILD_MEMBER_NOT_FOUND));

        final String accessToken =
                jwtUtil.createAccessToken(user, botId);
        final String refreshToken =
                jwtUtil.createRefreshToken(user, botId);

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

    public void logout(
            CustomUser user,
            final HttpServletResponse response
    ) {
        String username = user.getUsername();

        sessionRepository.invalidate(username);

        response.addHeader(
                "Set-Cookie",
                jwtUtil.createExpiredAccessTokenCookie()
                        .toString()
        );

        response.addHeader(
                "Set-Cookie",
                jwtUtil.createExpiredRefreshTokenCookie()
                        .toString()
        );

        ApiResponse.success(null);
    }
}
