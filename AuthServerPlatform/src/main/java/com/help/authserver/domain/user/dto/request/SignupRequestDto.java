package com.help.authserver.domain.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SignupRequestDto {
	@NotBlank(message = "이메일을 입력해주세요")
	@Email(message = "올바른 이메일 형식으로 작성해주세요")
	private String email;

	@NotBlank(message = "비밀번호를 입력해주세요")
	@Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하여야 합니다")
	@Pattern(
		regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+=\\-{}\\[\\]:;\"'<>,.?/])[A-Za-z\\d!@#$%^&*()_+=\\-{}\\[\\]:;\"'<>,.?/]*$",
		message = "비밀번호는 영문 대문자, 영문 소문자, 숫자, 특수문자를 포함해야 하며 공백이 포함될 수 없습니다"
	)
	private String password;

	@NotBlank(message = "닉네임을 입력해주세요")
	@Size(min = 2, max = 10, message = "닉네임은 2자 이상 10자 이하여야 합니다")
	private String nickname;
}
