package com.help.authserver.domain.user.dto.response;

public record LoginResponseDto(
	String accessToken,
	String redirectUrl
) {
}
