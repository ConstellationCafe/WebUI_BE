package com.help.authserver.domain.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequestDto {
	@Email(message = "이메일 형식으로 작성해주세요")
	private String email;
	@NotBlank(message = "비밀번호를 입력해주세요")
	private String password;
}
