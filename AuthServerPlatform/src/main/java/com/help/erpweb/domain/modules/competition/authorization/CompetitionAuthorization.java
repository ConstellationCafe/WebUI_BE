package com.help.erpweb.domain.modules.competition.authorization;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.help.global.data.Authority;
import com.help.global.discord.identity.DiscordUserRepository;
import com.help.global.guild.GuildContext;

import lombok.RequiredArgsConstructor;

/**
 * 대회 기능 권한. 아카데미({@code academyAuth})처럼 컨트롤러에서
 * {@code @PreAuthorize("@competitionAuth.isManager(authentication)")}로 쓴다.
 *
 * <p>현재 채팅방(botId)의 RoleTable에 {@value #MANAGER_ROLE_KEYWORD}가 들어간 역할이 있으면 대회 매니저다.
 * 역할 이름은 봇이 디스코드 역할명을 그대로 기록하므로 "섀버 대회 매니저"처럼 앞뒤에 다른 글자가 붙어도 인정한다.
 * 서버장(ADMIN)은 아카데미와 같이 모든 대회 기능을 쓸 수 있다.
 */
@Component("competitionAuth")
@RequiredArgsConstructor
public class CompetitionAuthorization {
	public static final String MANAGER_ROLE_KEYWORD = "대회 매니저";

	private final DiscordUserRepository discordUserRepository;

	public boolean isManager(Authentication authentication) {
		if (!isAuthenticated(authentication)) {
			return false;
		}
		if (isAdmin(authentication)) {
			return true;
		}
		// 현재 채팅방의 재적 회원만 조회한다(ADR-0001). roles는 EntityGraph로 함께 읽힌다.
		return discordUserRepository.findByBotIdAndDiscordID(GuildContext.requireBotId(), authentication.getName())
			.map(user -> user.getRoles().stream()
				.anyMatch(role -> role != null && role.contains(MANAGER_ROLE_KEYWORD)))
			.orElse(false);
	}

	private static boolean isAdmin(Authentication authentication) {
		return authentication.getAuthorities().stream()
			.anyMatch(authority -> Authority.ADMIN.equals(authority.getAuthority()));
	}

	private static boolean isAuthenticated(Authentication authentication) {
		return authentication != null
			&& authentication.isAuthenticated()
			&& authentication.getName() != null;
	}
}
