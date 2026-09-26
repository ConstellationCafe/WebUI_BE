package com.help.authserver.domain.user.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.help.authserver.api.LoginAPI;
import com.help.authserver.domain.user.dto.discord.DiscordGuildDto;
import com.help.authserver.domain.user.dto.user.CurrentUserDto;
import com.help.authserver.domain.user.dto.user.DiscordUserDto;
import com.help.authserver.domain.user.entity.SessionInfo;
import com.help.authserver.domain.user.repository.SessionRepository;
import com.help.global.common.exception.CustomException;
import com.help.global.common.exception.ErrorCode;
import com.help.global.common.response.ApiResponse;
import com.help.global.discord.config.ERPSubscriberRepository;
import com.help.global.discord.config.ErpSubscriber;
import com.help.global.discord.constellation.DiscordUserRepository;
import com.help.global.jwt.CustomUser;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * Discord OAuth 전용 로그인/프로필 조회. JWT 발급·Redis 세션·쿠키 같은
 * 프로바이더 무관 로직은 {@link AuthSessionService}에 위임한다(login()의
 * 마지막 단계). me()/guilds()는 Discord API에서 받아오는 프로필/guild
 * 정보를 그대로 반환하는 응답이라 프로바이더 무관화하지 않고 여기 남겨뒀다
 * — 추후 두 번째 프로바이더가 추가되면 그 프로바이더도 자기 자신의
 * me()/guilds() 상당 메서드를 따로 가지게 될 것이다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DiscordLoginService implements OAuthLoginService, UserDetailsService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final LoginAPI<DiscordUserDto> loginAPI;
    private final DiscordUserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final ERPSubscriberRepository erpSubscriberRepository;
    private final AuthSessionService authSessionService;

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
                .map(DiscordUserMapper::toCustomUser)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "UserDetails creation failed. discordID=" + discordID
                        )
                );
    }

    @Override
    public String login(final String code, final HttpServletResponse response) {
        try {
            final String discordAccessToken = loginAPI.exchangeCodeForToken(code);
            final DiscordUserDto userDto = loginAPI.getUserInfo(discordAccessToken);

            final CustomUser identity =
                    (CustomUser) loadUserByUsername(userDto.discordId());

            authSessionService.issueSession(identity, discordAccessToken, response);

            return redirectUri;
        } catch (UsernameNotFoundException e) {
            return registerUri;
        }
    }

    public ApiResponse<?> me(final CustomUser user) {
        final String username = user.getUsername();

        final SessionInfo sessionInfo =
                sessionRepository.find(username)
                        .orElseThrow(() -> new CustomException(ErrorCode.SESSION_EXPIRED));

        if (!sessionInfo.isValid()) {
            throw new CustomException(ErrorCode.SESSION_REVOKED);
        }

        final String discordAccessToken = sessionInfo.getDiscordAccessToken();

        if (discordAccessToken == null) {
            throw new CustomException(ErrorCode.SESSION_EXPIRED);
        }

        final DiscordUserDto userDto = getUserInfo(discordAccessToken);

        final List<String> roles =
                user.getAuthorities()
                        .stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList();

        final CurrentUserDto meDto = new CurrentUserDto(
                userDto.discordId(),
                userDto.username(),
                userDto.globalName(),
                userDto.avatar(),
                roles
        );

        return ApiResponse.success(meDto);
    }

    public ApiResponse<?> guilds(final CustomUser user) {
        final String username = user.getUsername();

        final SessionInfo sessionInfo =
                sessionRepository.find(username)
                        .orElseThrow(() -> new CustomException(ErrorCode.SESSION_EXPIRED));

        final String discordAccessToken = sessionInfo.getDiscordAccessToken();

        try {
            final DiscordUserDto userDto = getUserInfo(discordAccessToken);

            final List<String> registeredGuildIds =
                    erpSubscriberRepository
                            .findByGuildIdIn(
                                    userDto.guilds().stream().map(DiscordGuildDto::id).toList()
                            )
                            .stream()
                            .map(ErpSubscriber::getGuildId)
                            .toList();

            final List<DiscordGuildDto> guilds =
                    userDto.guilds().stream()
                            .filter(guild -> registeredGuildIds.contains(guild.id()))
                            .toList();

            return ApiResponse.success(guilds);
        } catch (Exception e) {
            log.error("Discord guild 조회 실패. 저장된 guild 목록으로 대체합니다", e);

            final List<DiscordGuildDto> guilds =
                    erpSubscriberRepository
                            .findByDiscordId(username)
                            .stream()
                            .map(subscriber ->
                                    new DiscordGuildDto(subscriber.getGuildId(), null, null, 0)
                            )
                            .toList();

            return ApiResponse.success(guilds);
        }
    }

    private DiscordUserDto getUserInfo(final String discordAccessToken) {
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

    private long resolveRetryAfter(final HttpClientErrorException.TooManyRequests exception) {
        final String retryAfter = exception.getResponseHeaders() == null
                ? null
                : exception.getResponseHeaders().getFirst("Retry-After");
        try {
            return Math.min(Math.max(Long.parseLong(retryAfter), 1L), 5L);
        } catch (Exception ignored) {
            try {
                final JsonNode body = OBJECT_MAPPER.readTree(exception.getResponseBodyAsString());
                return Math.min(Math.max((long) Math.ceil(body.path("retry_after").asDouble(1.0)), 1L), 5L);
            } catch (Exception parseException) {
                return 1L;
            }
        }
    }

    private void sleep(final long seconds) {
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new CustomException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
        }
    }
}
