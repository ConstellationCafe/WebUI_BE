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

	// User 인터페이스로부터 생성
	public static CustomUser from(final com.help.authserver.domain.user.entity.User user) {
		final List<GrantedAuthority> authorities = new ArrayList<>();
		authorities.add(new SimpleGrantedAuthority(user.getRole().getRoleName()));
		final CustomUser customUser = new CustomUser(
			user.getUsername(),
			user.getPassword(),
			authorities
		);
		customUser.setUserId(user.getUserId());
//		customUser.setNickname(user.getUserProfile().getNickname());  // DiscordUser는 Profile이 없음

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
