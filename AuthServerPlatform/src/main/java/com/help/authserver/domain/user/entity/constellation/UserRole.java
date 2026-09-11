package com.help.authserver.domain.user.entity.constellation;

import com.help.global.data.Authority;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserRole {
	ADMIN(Authority.ADMIN),
	USER(Authority.USER);

	private final String roleName;
}

