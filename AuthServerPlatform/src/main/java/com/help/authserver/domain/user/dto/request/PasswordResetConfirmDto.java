package com.help.authserver.domain.user.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmDto(
	String token,
	@Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하여야 합니다")
	@Pattern(
		regexp = PasswordResetConfirmDto.PASSWORD_PATTERN,
		message = "비밀번호는 영문 대문자, 영문 소문자, 숫자, 특수문자를 포함해야 하며 공백이 포함될 수 없습니다"
	)
	String newPassword
) {
	private static final String PASSWORD_PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+=\\-{}\\[\\]:;\"'<>,.?/])[A-Za-z\\d!@#$%^&*()_+=\\-{}\\[\\]:;\"'<>,.?/]*$";
}
