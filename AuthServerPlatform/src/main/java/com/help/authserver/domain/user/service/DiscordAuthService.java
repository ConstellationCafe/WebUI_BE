package com.help.authserver.domain.user.service;
import com.help.authserver.api.LoginAPI;
import com.help.authserver.domain.user.dto.response.LoginResponseDto;
import com.help.authserver.domain.user.dto.user.DiscordUserDto;
import com.help.authserver.domain.user.repository.DiscordMembershipRepository;
import com.help.authserver.domain.user.repository.DiscordUserRepository;
import com.help.authserver.domain.user.view.DiscordMembershipView;
import com.help.authserver.global.common.exception.ErrorCode;
import com.help.authserver.global.common.response.ApiResponse;
import com.help.authserver.global.jwt.CustomUser;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import com.help.authserver.global.jwt.JwtUtil;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DiscordAuthService implements UserDetailsService  {
    private JwtUtil jwtUtil;
    private LoginAPI<DiscordUserDto> loginAPI;
    private DiscordUserRepository userRepository;
    private DiscordMembershipRepository membershipRepository;

    @Override
    public UserDetails loadUserByUsername(final String discordID) throws UsernameNotFoundException {
        // 여기서 말하는 Username 은 discordID에 해당
        List<DiscordMembershipView> membershipViews = membershipRepository.findByDiscordID(discordID);
        CustomUser user = CustomUser.from(membershipViews);

        if (user != null) {
            return user;
        } else {
            throw new UsernameNotFoundException("UserDetails creation failed. discordID=" + discordID);
        }
    }

    public ApiResponse<?> login(
            final String code,
            final String redirectUrl,
            final HttpServletResponse response
    ) {
        // oauth를 통해 유저 정보 가져오기
        DiscordUserDto userDto = loginAPI.getUserInfo(code);
        // vw_DiscordMembership 데이터로 Security Context(CustomUser) 생성
        List<DiscordMembershipView> membershipViews = membershipRepository.findByDiscordID(userDto.discordId());
        CustomUser user = CustomUser.from(membershipViews);

        if (user != null) {
            // JWT 토큰 생성
            final String accessToken = jwtUtil.createAccessToken(user);
            final String refreshToken = jwtUtil.createRefreshToken(user);

            // JWT 토큰 반환
            response.addHeader("Set-Cookie", jwtUtil.createRefreshTokenCookie(refreshToken).toString());
            response.addHeader("Authorization", accessToken);
            final LoginResponseDto responseDto = new LoginResponseDto(accessToken, redirectUrl);
            return ApiResponse.success(responseDto);
        } else {
            return ApiResponse.error(ErrorCode.USER_NOT_FOUND);
        }
    }
}
