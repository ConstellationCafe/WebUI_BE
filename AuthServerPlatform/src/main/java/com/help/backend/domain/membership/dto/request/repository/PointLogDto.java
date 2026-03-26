package com.help.backend.domain.membership.dto.request.repository;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointLogDto {
    @NotBlank(message = "amount 필수입니다.")
    private String amount;
    @NotBlank(message = "at 필수입니다.")
    private String at;
    private String description;
}
