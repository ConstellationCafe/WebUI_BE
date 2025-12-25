//package com.help.authserver.domain.user.service;
//
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.core.userdetails.UsernameNotFoundException;
//import org.springframework.stereotype.Service;
//
//import com.help.authserver.domain.user.dto.request.LoginRequestDto;
//import com.help.authserver.domain.user.dto.response.LoginResponseDto;
//import com.help.authserver.domain.user.entity.User;
//import com.help.authserver.domain.user.repository.UserRepository;
//import com.help.authserver.global.common.exception.CustomException;
//import com.help.authserver.global.common.exception.ErrorCode;
//import com.help.authserver.global.common.response.ApiResponse;
//import com.help.authserver.global.jwt.CustomUser;
//import com.help.authserver.global.jwt.JwtUtil;
//
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//
//@Service
//@Slf4j
//@RequiredArgsConstructor
//public class AuthService implements UserDetailsService {
//	private final UserRepository userRepository;
//	private final AuthenticationManagerBuilder authenticationManagerBuilder;
//	private final JwtUtil jwtUtil;
//
//	@Override
//	public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
//		final var user = userRepository.findWithProfileByUsername(username)
//			.orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다"));
//
//		return CustomUser.from(user);
//	}
//
//	public ApiResponse<?> login(
//		final LoginRequestDto loginRequestDto,
//		final String redirectUrl,
//		final HttpServletResponse response
//	) {
//		final Authentication authentication = authenticationManagerBuilder.getObject()
//			.authenticate(new UsernamePasswordAuthenticationToken(
//				loginRequestDto.getEmail(),
//				loginRequestDto.getPassword()
//			));
//
//		final CustomUser user = (CustomUser)authentication.getPrincipal();
//		final String accessToken = jwtUtil.createAccessToken(user);
//		final String refreshToken = jwtUtil.createRefreshToken(user);
//
//		response.addHeader("Set-Cookie", jwtUtil.createRefreshTokenCookie(refreshToken).toString());
//		response.addHeader("Authorization", accessToken);
//
//		final LoginResponseDto responseDto = new LoginResponseDto(accessToken, redirectUrl);
//		return ApiResponse.success(responseDto);
//	}
//
//	public ApiResponse<?> refresh(final HttpServletRequest request, final HttpServletResponse response) {
//		final User user = jwtUtil.extractRefreshTokenFromRequest(request)
//			.filter(jwtUtil::isTokenValidate)
//			.flatMap(jwtUtil::extractUsername)
//			.flatMap(userRepository::findWithProfileByUsername)
//			.orElseThrow(() -> new CustomException(ErrorCode.INVALID_TOKEN));
//
//		final String accessToken = jwtUtil.createAccessToken(CustomUser.from(user));
//		response.addHeader("Authorization", accessToken);
//		log.info("AccessToken 재발급: {}", accessToken);
//		return ApiResponse.success(accessToken);
//	}
//
//	// logout
//	public void logout(final HttpServletResponse response) {
//		response.addHeader("Set-Cookie", jwtUtil.createExpiredRefreshTokenCookie().toString());
//		ApiResponse.success(null);
//	}
//
//	public Boolean checkLogin(final HttpServletRequest request) {
//		return jwtUtil.extractAccessTokenFromRequest(request)
//			.filter(jwtUtil::isTokenValidate).isPresent();
//	}
//}
