package com.help.global.jwt;

import com.help.global.discord.identity.DiscordUser;
import com.help.global.discord.identity.DiscordUserRepository;
import com.help.global.guild.GuildContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.PatternMatchUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
@Component
public class BackEndJwtAuthFilter extends OncePerRequestFilter {
	// 요청에 포함된 JWT를 검사하고, 인증된 사용자 정보(SecurityContext)를 설정
	private record WhiteListEntry(String method, String uriPattern) { }

	// 인증이 필요 없는 API 요청을 허용
	private static final List<WhiteListEntry> WHITE_LIST = List.of(
//		new WhiteListEntry("GET", "/"),
//		new WhiteListEntry("GET", "/api/auth/verify"),
//		new WhiteListEntry("POST", "/api/auth/login"),
//		new WhiteListEntry("POST", "/api/auth/logout")
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
		String uri = request.getRequestURI();
		return uri.startsWith("/auth/");
	}

	private boolean isWhiteListed(final String method, final String uri) {
		return WHITE_LIST.stream()
			.anyMatch(entry ->
				entry.method().equalsIgnoreCase(method)
					&& PatternMatchUtils.simpleMatch(entry.uriPattern(), uri)
			);
	}

	private void checkAccessTokenAndAuthentication(final HttpServletRequest request,
												   final HttpServletResponse response,
												   final FilterChain filterChain) throws ServletException, IOException {
		// ADR-0001: /api/**는 "채팅방까지 선택해 로그인이 완료된" 토큰만 인정한다.
		// botId claim이 없는 토큰(=discordId 인증만 끝난 "선택 대기" 상태)은
		// 인증 정보를 저장하지 않고 그대로 통과시켜, 이후 authorizeHttpRequests()의
		// anyRequest().authenticated()가 401(UNAUTHORIZED)로 걸러내게 한다.
		final Optional<String> accessToken = jwtUtil.extractAccessTokenFromRequest(request)
			.filter(jwtUtil::isTokenValidate);

		final Optional<String> botId = accessToken.flatMap(jwtUtil::extractBotId);

		// ADR-0001: admin 여부(RoleTable)는 방(botId) 단위로 재정의되었으므로,
		// discordID만으로 조회하지 않고 반드시 botId까지 스코프해서 조회한다.
		// 이 조회가 비면(=등록된 방이지만 이 사용자가 멤버가 아님) 인증 정보를
		// 저장하지 않고 그대로 흘려보내 401로 걸러지게 한다.
		if (accessToken.isPresent() && botId.isPresent()) {
			accessToken.flatMap(jwtUtil::extractUsername)
				.flatMap(username -> userRepository.findByBotIdAndDiscordID(botId.get(), username))
				.ifPresent(discordUser -> {
					GuildContext.setBotId(botId.get());
					saveAuthentication(discordUser);
				});
		}

		try {
			filterChain.doFilter(request, response);
		} finally {
			GuildContext.clear();
		}
	}

	private void saveAuthentication(final DiscordUser discordUser) {
		final UserDetails userDetails = CustomUser.of(
			discordUser.getUsername(),
			discordUser.getPassword(),
			discordUser.getRoleName(),
			null
		);
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
