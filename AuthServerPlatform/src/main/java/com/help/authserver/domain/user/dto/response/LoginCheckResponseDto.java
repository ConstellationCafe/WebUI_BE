package com.help.authserver.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginCheckResponseDto {
	private Boolean isLogin;
	private Boolean refreshHint;
	// ADR-0001: discordId 인증은 끝났지만 아직 채팅방을 선택하지 않은
	// 상태(AccessToken/RefreshToken에 botId claim이 없음)면 false.
	// FE는 이 값이 false면 /select로 보내야 한다.
	private Boolean roomSelected;
}
