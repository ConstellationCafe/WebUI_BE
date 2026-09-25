package com.help.global.jwt;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import com.help.authserver.domain.user.entity.constellation.DiscordUser;
import com.help.authserver.domain.user.entity.constellation.User;
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
		log.debug("인증 필터 시작: [{}]{}", method, requestUri);
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
	private void checkAccessTokenAndAuthentication(
			final HttpServletRequest request,
			final HttpServletResponse response,
			final FilterChain filterChain
	) throws ServletException, IOException {
		// Access Token 검증 및 username 추출
		final Optional<String> username = jwtUtil.extractAccessTokenFromRequest(request)
				.filter(jwtUtil::isTokenValidate)
				.flatMap(jwtUtil::extractUsername);
		if (username.isEmpty()) {
			log.warn("유효하지 않은 Access Token 또는 username 추출 실패");
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			return;
		}
		// 세션 존재 여부 및 유효성 검증
		final Optional<SessionInfo> sessionInfo = sessionRepository.find(username.get())
				.filter(SessionInfo::isValid);
		if (sessionInfo.isEmpty()) {
			log.warn("유효하지 않은 세션 또는 세션 없음");
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			return;
		}
		// Discord 사용자 존재 여부 검증
		// ADR-0001: /auth/**는 채팅방(botId) 선택 이전 구간이라 아직 스코프가 없다.
		// 신원 확인 목적으로만 사용하며, 이 결과의 roles(방별 admin 여부)는
		// 사용하지 않는다(saveAuthentication이 만드는 CustomUser의 authority는
		// 실제 인가 판단에 쓰이지 않고, /api/**의 진짜 인가는 BackEndJwtAuthFilter가
		// botId로 다시 조회해서 수행한다).
		final Optional<DiscordUser> user = userRepository.findIdentityByDiscordID(username.get());
		if (user.isEmpty()) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			return;
		}
	 	// 모든 인증 검증 통과
		saveAuthentication(user.get());
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
		log.debug("인증 정보를 Security Context에 저장했습니다");
	}
}
