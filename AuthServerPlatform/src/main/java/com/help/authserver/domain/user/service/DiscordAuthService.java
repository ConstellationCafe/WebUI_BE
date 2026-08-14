package com.help.authserver.domain.user.service;
import com.help.authserver.api.LoginAPI;
import com.help.authserver.domain.user.dto.discord.DiscordGuildDto;
import com.help.authserver.domain.user.dto.response.LoginCheckResponseDto;
import com.help.authserver.domain.user.dto.user.CurrentUserDto;
import com.help.authserver.domain.user.dto.user.DiscordUserDto;
import com.help.authserver.domain.user.entity.constellation.DiscordUser;
import com.help.authserver.domain.user.entity.config.ErpSubscriber;
import com.help.authserver.domain.user.entity.SessionInfo;
import com.help.authserver.domain.user.repository.constellation.DiscordUserRepository;
import com.help.authserver.domain.user.repository.config.ERPSubscriberRepository;
import com.help.authserver.domain.user.repository.SessionRepository;
import com.help.global.common.exception.CustomException;
import com.help.global.common.exception.ErrorCode;
import com.help.global.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import com.help.global.jwt.CustomUser;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import com.help.global.jwt.JwtUtil;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class DiscordAuthService implements UserDetailsService  {
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
    public UserDetails loadUserByUsername(final String discordID) throws UsernameNotFoundException {
        // 여기서 말하는 Username 은 discordID에 해당
        return userRepository.findByDiscordID(discordID)
                .map(CustomUser::from)
                .orElseThrow(() ->
                        new UsernameNotFoundException("UserDetails creation failed. discordID=" + discordID));
    }

    @Transactional
    public String login(
            final String code,
            final HttpServletResponse response
    ) {
        try {
            // oauth를 통해 유저 정보 가져오기
            String accessTokenForDiscord = loginAPI.exchangeCodeForToken(code);
            DiscordUserDto userDto = loginAPI.getUserInfo(accessTokenForDiscord);
            UserDetails discordUser = loadUserByUsername(userDto.discordId());
            // JWT 토큰 생성
            String accessToken = jwtUtil.createAccessToken((CustomUser) discordUser);
            String refreshToken = jwtUtil.createRefreshToken((CustomUser) discordUser);
            // Redis 세션 저장
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
            // JWT 토큰 반환 (쿠키에 담아서)
            response.addHeader("Set-Cookie", jwtUtil.createAccessTokenCookie(accessToken).toString());
            response.addHeader("Set-Cookie", jwtUtil.createRefreshTokenCookie(refreshToken).toString());
            return redirectUri;

        } catch (UsernameNotFoundException e) {
            return registerUri;
        }
    }

    public ApiResponse<?> me(CustomUser user) {
        String username = user.getUsername();
        SessionInfo sessionInfo = sessionRepository.find(username)
                .orElseThrow(() -> new CustomException(ErrorCode.SESSION_EXPIRED));

//        if (sessionInfo == null)
//            throw new RuntimeException("Session expired");
        if (!sessionInfo.isValid())
            throw new RuntimeException("Session revoked");

        String discordAccessToken = sessionInfo.getDiscordAccessToken();
        if (discordAccessToken == null)
            throw new RuntimeException("Discord token missing");

        DiscordUserDto userDto = loginAPI.getUserInfo(discordAccessToken);
        List<String> roles = user.getAuthorities().stream()
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
        SessionInfo sessionInfo = sessionRepository.find(username)
                .orElseThrow(() -> new CustomException(ErrorCode.SESSION_EXPIRED));
        String discordAccessToken = sessionInfo.getDiscordAccessToken();
        try {
            DiscordUserDto userDto = getUserInfoWithRetry(discordAccessToken, username);
            List<String> registeredGuildIds = erpSubscriberRepository
                    .findByGuildIdIn(
                            userDto.guilds()
                                    .stream()
                                    .map(DiscordGuildDto::id)
                                    .toList()
                    )
                    .stream()
                    .map(ErpSubscriber::getGuildId)
                    .toList();
            List<DiscordGuildDto> guilds = userDto.guilds()
                    .stream()
                    .filter(guild -> registeredGuildIds.contains(guild.id()))
                    .toList();
            return ApiResponse.success(guilds);
        } catch (Exception e) {
            log.error(
                    "Discord guild 조회 실패 - username={}, message={}",
                    username,
                    e.getMessage(),
                    e
            );
            List<DiscordGuildDto> guilds = erpSubscriberRepository
                    .findByDiscordId(username)
                    .stream()
                    .map(subscriber -> new DiscordGuildDto(
                            subscriber.getGuildId(),
                            null,
                            null,
                            0
                    ))
                    .toList();
            return ApiResponse.success(guilds);
        }
    }

    private DiscordUserDto getUserInfoWithRetry(
            String discordAccessToken,
            String username
    ) {
        final int maxRetries = 1;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return loginAPI.getUserInfo(discordAccessToken);
            } catch (HttpClientErrorException.TooManyRequests e) {
                if (attempt >= maxRetries) {
                    log.error(
                            "Discord API Rate Limit 재시도 실패 - username={}, attempts={}",
                            username,
                            attempt + 1
                    );
                    throw e;
                }
                double retryAfter = extractRetryAfter(e);
                log.warn(
                        "Discord API Rate Limit 발생 - username={}, retryAfter={}초, 재시도={}/{}",
                        username,
                        retryAfter,
                        attempt + 1,
                        maxRetries
                );
                sleep(retryAfter);
            }
        }
        throw new IllegalStateException("Discord API 요청 실패");
    }

    private double extractRetryAfter(
            HttpClientErrorException.TooManyRequests e
    ) {
        try {
            JsonNode json = new ObjectMapper()
                    .readTree(e.getResponseBodyAsString());
            return json.path("retry_after").asDouble(1.0);
        } catch (Exception parseException) {
            log.warn(
                    "Discord 429 응답에서 retry_after 파싱 실패. 기본값 1초 사용",
                    parseException
            );
            return 1.0;
        }
    }

    private void sleep(double seconds) {
        try {
            long millis = (long) Math.ceil(seconds * 1000);
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Discord API 재시도 대기 중 인터럽트 발생",
                    e
            );
        }
    }

    public ApiResponse<?> checkLogin(final HttpServletRequest request) {
        boolean isLogin = jwtUtil.extractAccessTokenFromRequest(request)
                .filter(jwtUtil::isTokenValidate)
                .isPresent();

        boolean refreshHint = jwtUtil.extractRefreshTokenFromRequest(request)
                .filter(jwtUtil::isTokenValidate)
                .isPresent();

        return ApiResponse.success(new LoginCheckResponseDto(isLogin, refreshHint));
    }

    // TODO : access Token 재발급시 refresh Token도 교체하기
    public ApiResponse<?> refresh(final HttpServletRequest request, final HttpServletResponse response) {
		final DiscordUser user = jwtUtil.extractRefreshTokenFromRequest(request)
			.filter(jwtUtil::isTokenValidate)
			.flatMap(jwtUtil::extractUsername)
			.flatMap(userRepository::findByDiscordID)
			.orElseThrow(() -> new CustomException(ErrorCode.INVALID_TOKEN));

        String username = user.getDiscordID();
        SessionInfo sessionInfo = sessionRepository.find(username)
                .orElseThrow(() -> new CustomException(ErrorCode.SESSION_EXPIRED));
        if (!sessionInfo.isValid()) {
            throw new CustomException(ErrorCode.SESSION_REVOKED);
        }

        final String accessToken = jwtUtil.createAccessToken(CustomUser.from(user));
        ResponseCookie accessCookie = jwtUtil.createAccessTokenCookie(accessToken);
        response.addHeader("Set-Cookie", accessCookie.toString());
		return ApiResponse.success(true);
	}

	public void logout(CustomUser user, final HttpServletResponse response) {
        String username = user.getUsername();
        sessionRepository.invalidate(username);
        response.addHeader("Set-Cookie", jwtUtil.createExpiredAccessTokenCookie().toString());
		response.addHeader("Set-Cookie", jwtUtil.createExpiredRefreshTokenCookie().toString());
		ApiResponse.success(null);
	}
}
