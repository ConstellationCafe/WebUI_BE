package com.help.authserver.domain.user.service;
import com.help.authserver.api.LoginAPI;
import com.help.authserver.domain.user.dto.response.LoginCheckResponseDto;
import com.help.authserver.domain.user.dto.response.LoginResponseDto;
import com.help.authserver.domain.user.dto.user.DiscordUserDto;
import com.help.authserver.domain.user.entity.DiscordUser;
//import com.help.authserver.domain.user.repository.DiscordMembershipRepository;
import com.help.authserver.domain.user.entity.SessionInfo;
import com.help.authserver.domain.user.repository.DiscordUserRepository;
import com.help.authserver.domain.user.repository.SessionRepository;
import com.help.global.common.exception.CustomException;
import com.help.global.common.exception.ErrorCode;
import com.help.global.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import com.help.global.jwt.CustomUser;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import com.help.global.jwt.JwtUtil;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DiscordAuthService implements UserDetailsService  {
    private final JwtUtil jwtUtil;
    private final LoginAPI<DiscordUserDto> loginAPI;
    private final DiscordUserRepository userRepository;
    private final SessionRepository sessionRepository;

    @Value("${front.redirect-uri}")
    private String redirectUri;

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
//            String state,
            final HttpServletResponse response
    ) {
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
            jwtUtil.getAccessTokenTtl()
        );
        // JWT 토큰 반환 (쿠키에 담아서)
        response.addHeader("Set-Cookie", jwtUtil.createAccessTokenCookie(accessToken).toString());
        response.addHeader("Set-Cookie", jwtUtil.createRefreshTokenCookie(refreshToken).toString());
//        response.addHeader("Authorization", accessToken);
//        final LoginResponseDto responseDto = new LoginResponseDto(accessToken, redirectUrl);
//        return ApiResponse.success(responseDto);
        return redirectUri;
    }

    public ApiResponse<?> me(CustomUser user) {
        String username = user.getUsername();
        SessionInfo sessionInfo = sessionRepository.find(username)
                .orElseThrow(() -> new CustomException(ErrorCode.SESSION_EXPIRED));

        if (sessionInfo == null)
            throw new RuntimeException("Session expired");
        if (!sessionInfo.isValid())
            throw new RuntimeException("Session revoked");

        String discordAccessToken = sessionInfo.getDiscordAccessToken();
        if (discordAccessToken == null)
            throw new RuntimeException("Discord token missing");

        DiscordUserDto userDto = loginAPI.getUserInfo(discordAccessToken);
        return ApiResponse.success(userDto);
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
        sessionRepository.refreshTtl(username, jwtUtil.getAccessTokenTtl());

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
