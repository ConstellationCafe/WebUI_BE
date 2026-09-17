package com.help.erpweb.domain.learning.dto.request.repository;

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

    /*
     * API에서는 Discord ID를 사용한다.
     * 실제 DB에는 Stored Procedure에서 SK로 변환하여 저장한다.
     */
    @NotBlank(message = "teacher는 필수입니다.")
    private String teacher;
}