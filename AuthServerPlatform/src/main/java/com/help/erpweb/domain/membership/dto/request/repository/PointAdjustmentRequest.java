package com.help.erpweb.domain.membership.dto.request.repository;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointAdjustmentRequest {
    @NotNull
    private PointAdjustmentType type;

    @NotNull
    @Positive
    private Integer amount;

    @NotBlank
    @Size(max = 1000)
    private String description;
}
