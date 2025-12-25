package com.help.authserver.domain.user.dto.request;

import java.util.Optional;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProfileUpdateRequestDto {
	@Size(min = 2, max = 10, message = "닉네임은 2자 이상 10자 이하여야 합니다")
	private String nickname;
	private Optional<String> description = Optional.empty();
}
