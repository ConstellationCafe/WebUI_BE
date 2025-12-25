package com.help.backend.global.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

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

	// db entity로부터 생성, userid와 nickname 사용
//	public static CustomUser from(final com.help.authserver.domain.user.entity.User user) {
//		final List<GrantedAuthority> authorities = new ArrayList<>();
//		authorities.add(new SimpleGrantedAuthority(user.getRole().getRoleName()));
//		final CustomUser customUser = new CustomUser(
//			user.getUsername(),
//			user.getPassword(),
//			authorities
//		);
//		customUser.setUserId(user.getUserId());
//		customUser.setNickname(user.getUserProfile().getNickname());
//
//		return customUser;
//	}
}
