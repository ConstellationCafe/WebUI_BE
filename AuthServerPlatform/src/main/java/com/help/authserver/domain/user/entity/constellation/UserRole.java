package com.help.authserver.domain.user.entity.constellation;

import com.help.global.data.Authority;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserRole {
	USER(Authority.user),
	ADMIN(Authority.admin);

	private final String roleName;
}

