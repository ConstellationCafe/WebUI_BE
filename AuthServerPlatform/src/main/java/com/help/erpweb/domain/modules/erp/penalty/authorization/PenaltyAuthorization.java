package com.help.erpweb.domain.modules.erp.penalty.authorization;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.help.global.data.Authority;
import com.help.global.discord.identity.DiscordUserRepository;
import com.help.global.guild.GuildContext;

import lombok.RequiredArgsConstructor;

/**
 * 벌점 관리 권한(ADR-0006). 대회({@code competitionAuth})처럼 컨트롤러에서
 * {@code @PreAuthorize("@penaltyAuth.isManager(authentication)")}로 쓴다.
 *
 * <p>현재 채팅방(botId)의 RoleTable에 {@link #MANAGER_ROLE_KEYWORDS} 중 하나가 들어간 역할이 있으면 벌점을 관리할 수 있다.
 * 봇이 디스코드 역할명을 그대로 기록하므로 앞뒤에 다른 글자가 붙어도 인정한다.
 * 서버장(ADMIN)은 역할과 무관하게 관리할 수 있다.
 */
@Component("penaltyAuth")
@RequiredArgsConstructor
public class PenaltyAuthorization {
	public static final List<String> MANAGER_ROLE_KEYWORDS = List.of("운영 매니저", "운영 본부원");

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
			.map(user -> user.getRoles().stream().anyMatch(PenaltyAuthorization::isManagerRole))
			.orElse(false);
	}

	private static boolean isManagerRole(String role) {
		return role != null && MANAGER_ROLE_KEYWORDS.stream().anyMatch(role::contains);
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
