package com.help.authserver.domain.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasswordResetRequestDto(
	@NotBlank(message = "이메일을 입력해주세요")
	@Email(message = "올바른 이메일 형식으로 작성해주세요")
	String email
) {
}

