package com.help.authserver.domain.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasswordResetRequestDto(
	@NotBlank(message = "ì´ë©ì¼ì ìë ¥í´ì£¼ì¸ì")
	@Email(message = "ì¬ë°ë¥¸ ì´ë©ì¼ íìì¼ë¡ ìì±í´ì£¼ì¸ì")
	String email
) {
}
