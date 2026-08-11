package com.help.global.jwt;

import java.io.IOException;
import java.util.List;

import com.help.authserver.domain.user.entity.User;
import com.help.authserver.domain.user.repository.constellation.DiscordUserRepository;
import com.help.authserver.domain.user.repository.SessionRepository;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.PatternMatchUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.help.authserver.domain.user.entity.SessionInfo;

@RequiredArgsConstructor
@Slf4j
@Component
public class AuthServerJwtAuthFilter extends OncePerRequestFilter {
	private final SessionRepository sessionRepository;

	// 요청에 포함된 JWT를 검사하고, 인증된 사용자 정보(SecurityContext)를 설정
	private record WhiteListEntry(String method, String uriPattern) {
	}

	// 인증이 필요 없는 API 요청을 허용
	private static final List<WhiteListEntry> WHITE_LIST = List.of(
//		new WhiteListEntry("GET", "/"),
		new WhiteListEntry("GET", "/auth/discord_login"),
		new WhiteListEntry("GET", "/auth/check"),
		new WhiteListEntry("POST", "/auth/refresh")
//		new WhiteListEntry("POST", "/auth/login"),
//		new WhiteListEntry("POST", "/auth/logout")
	);

	private final JwtUtil jwtUtil;
	private final DiscordUserRepository userRepository;

	@Override
	protected void doFilterInternal(
		@NonNull final HttpServletRequest request,
		@NonNull final HttpServletResponse response,
		@NonNull final FilterChain filterChain
	) throws ServletException, IOException {
		final String requestUri = request.getRequestURI();
		final String method = request.getMethod();

		// 화이트리스트에 해당되면 필터를 통과(인증 없이 허용)
		if (isWhiteListed(method, requestUri)) {
			filterChain.doFilter(request, response);
			return;
		}
		// 요청에서 JWT 토큰을 추출하고, 유효성을 검사하여 인증 정보를 저장
		if (shouldNotFilter(request)) {
			filterChain.doFilter(request, response);
			return;
		}
		log.info("인증 필터 시작: [{}]{}", method, requestUri);
		checkAccessTokenAndAuthentication(request, response, filterChain);
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return request.getRequestURI().startsWith("/api/");
	}

	// url 과 method 의 인증 필요 여부를 WHITE_LIST 에서 확인
	private boolean isWhiteListed(final String method, final String uri) {
		return WHITE_LIST.stream()
			.anyMatch(entry ->
				entry.method().equalsIgnoreCase(method)
					&& PatternMatchUtils.simpleMatch(entry.uriPattern(), uri)
			);
	}

	// FIXME : 로그인 종류에 따른 필터
	private void checkAccessTokenAndAuthentication(final HttpServletRequest request, final HttpServletResponse response,
		final FilterChain filterChain) throws ServletException, IOException {
		jwtUtil.extractAccessTokenFromRequest(request)
				.filter(jwtUtil::isTokenValidate)
				.flatMap(jwtUtil::extractUsername)
				.ifPresent(username -> {
					sessionRepository.find(username)
						.filter(SessionInfo::isValid)
						.ifPresentOrElse(sessionInfo -> {
							userRepository.findByDiscordID(username)
								.ifPresent(this::saveAuthentication);
						}, () -> {
							log.warn("유효하지 않은 세션 또는 세션 없음: {}", username);
						});
				});

		filterChain.doFilter(request, response);
	}

	private void saveAuthentication(final User user) {
		final UserDetails userDetails = CustomUser.from(user);
		final Authentication authentication =
			new UsernamePasswordAuthenticationToken(
				userDetails,
				null,
				userDetails.getAuthorities()
			);

		SecurityContextHolder.getContext().setAuthentication(authentication);
		log.info("Security Context에 '{}' 인증 정보를 저장", user.getUsername());
		log.info("isAuthenticated: {}", SecurityContextHolder.getContext().getAuthentication().isAuthenticated());
	}
}
