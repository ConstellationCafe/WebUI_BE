package com.help.authserver.domain.user.dto.response;

import com.help.authserver.domain.user.entity.constellation.UserProfile;

public record ProfileResponseDto(UserInfo user) {
	public static ProfileResponseDto from(final UserProfile userProfile) {
		return new ProfileResponseDto(new UserInfo(userProfile.getNickname(), userProfile.getEmail()));
	}

	private record UserInfo(String nickname, String email) {
	}
}
