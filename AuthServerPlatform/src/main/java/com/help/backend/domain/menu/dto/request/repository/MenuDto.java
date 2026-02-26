package com.help.backend.domain.menu.dto.request.repository;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuDto {
    @NotBlank(message = "mn_value는 필수입니다.")
    private String mnValue;

//    @NotBlank(message = "recommender는 필수입니다.")
//    private String recommender;
}
