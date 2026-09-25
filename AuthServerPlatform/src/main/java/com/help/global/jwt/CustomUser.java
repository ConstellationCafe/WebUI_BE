package com.help.global.jwt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import lombok.Getter;
import lombok.Setter;

public class CustomUser extends User {
	// User를 확장해서 추가 사용자 정보를 저장
	@Getter
	@Setter
	private UUID userId;
	@Getter
	@Setter
	private String nickname;

	// 생성자로 생성, userid와 nickname 사용 x
	public CustomUser(
		String username,
		String password,
		Collection<? extends GrantedAuthority> authorities
	) {
		super(username, password, authorities);  // username = discordId
	}

	// authserver/erpweb 서비스 분리 준비: CustomUser(global)가 authserver의
	// User 인터페이스에 의존하지 않도록, 필요한 원시 값만 받는 팩토리로 바꼈다.
	// 예전 CustomUser.from(User)는 global -> authserver 방향의 의존이었다.
	public static CustomUser of(
		final String username,
		final String password,
		final String roleName,
		final UUID userId
	) {
		final List<GrantedAuthority> authorities = new ArrayList<>();
		authorities.add(new SimpleGrantedAuthority(roleName));

		final CustomUser customUser = new CustomUser(
			username,
			password,
			authorities
		);
		customUser.setUserId(userId);

		return customUser;
	}

//	public static CustomUser from(final List<DiscordMembershipView> membershipViews) {
//		List<GrantedAuthority> authorities = new ArrayList<>();
//		if (membershipViews.isEmpty()) {
//			return null;
//		} else {
//			boolean hasWithdrawn = membershipViews.stream()
//					.map(DiscordMembershipView::getState)
//					.anyMatch("탈퇴"::equals);
//			if (hasWithdrawn) {
//				return null;
//			}
//
//			boolean isAdmin = membershipViews.stream()
//					.map(DiscordMembershipView::getRole)
//					.anyMatch("서버장"::equals);
//			if (isAdmin)
//				authorities.add(new SimpleGrantedAuthority(Authority.admin));
//			else
//				authorities.add(new SimpleGrantedAuthority(Authority.user));
//		}
//
//		return new CustomUser(
//			membershipViews.get(0).getDiscordID(),  // username 대용
//			"OAUTH_USER",
//			authorities
//		);
//	}
}
