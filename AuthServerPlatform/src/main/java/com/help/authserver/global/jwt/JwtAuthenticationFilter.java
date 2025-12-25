package com.help.authserver.global.jwt;

import java.io.IOException;
import java.util.List;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.PatternMatchUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.help.authserver.domain.user.entity.User;
import com.help.authserver.domain.user.repository.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
	// 요청에 포함된 JWT를 검사하고, 인증된 사용자 정보(SecurityContext)를 설정
	private record WhiteListEntry(String method, String uriPattern) {
	}

	// 인증이 필요 없는 API 요청을 허용
	private static final List<WhiteListEntry> WHITE_LIST = List.of(
		new WhiteListEntry("GET", "/"),
		new WhiteListEntry("GET", "/api/auth/verify"),
		new WhiteListEntry("POST", "/api/auth/login"),
		new WhiteListEntry("POST", "/api/auth/logout")
	);

	private final JwtUtil jwtUtil;
	private final UserRepository userRepository;

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
		log.info("인증 필터 시작: [{}]{}", method, requestUri);
		checkAccessTokenAndAuthentication(request, response, filterChain);
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
			.flatMap(jwtUtil::extractUsername)  // FIXME : discordID보단 membershipID 같은게 좋을 듯? -> 표준화
			.flatMap(userRepository::findWithProfileByUsername)  // FIXME : 일반화된 레포지터리 필요..?
			.ifPresent(this::saveAuthentication);

		filterChain.doFilter(request, response);
	}

	// FIXME : 로그인 방식에 따라 객체가 다르게 들어오지 않음?
	// Memo : User 인터페이스 : 이걸 기존 EmailUser, DiscordMembership 등이 구현하기 !
	private void saveAuthentication(final User myUser) {
		final UserDetails userDetails = CustomUser.from(myUser);  // Memo : 그럼 각 사용자별로로 from 정의
		final Authentication authentication =
			new UsernamePasswordAuthenticationToken(
				userDetails,
				null,
				userDetails.getAuthorities()
			);

		SecurityContextHolder.getContext().setAuthentication(authentication);
		log.info("Security Context에 '{}' 인증 정보를 저장", myUser.getUsername());
		log.info("isAuthenticated: {}", SecurityContextHolder.getContext().getAuthentication().isAuthenticated());
	}
}
