package com.help.backend.domain.learning.dto.request.repository;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearningDto {
    @NotBlank(message = "ln_key는 필수입니다.")
    private String lnKey;

    @NotBlank(message = "ln_value는 필수입니다.")
    private String lnValue;

//    @NotBlank(message = "teacher는 필수입니다.")
//    private String teacher;
}
