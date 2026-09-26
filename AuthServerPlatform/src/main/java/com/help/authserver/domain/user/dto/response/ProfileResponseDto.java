// [사용되지 않는 코드 - 주석 처리됨] 활성 호출부가 없어 비활성화함
//package com.help.authserver.domain.user.dto.response;
//
//import com.help.authserver.domain.user.entity.legacy.UserProfile;
//
//public record ProfileResponseDto(UserInfo user) {
//	public static ProfileResponseDto from(final UserProfile userProfile) {
//		return new ProfileResponseDto(new UserInfo(userProfile.getNickname(), userProfile.getEmail()));
//	}
//
//	private record UserInfo(String nickname, String email) {
//	}
//}
//
