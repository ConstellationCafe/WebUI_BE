package com.help.backend.domain.content.dto.request.repository;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentDto {
    @NotBlank(message = "cnValue는 필수입니다.")
    private String cnValue;

//    @NotBlank(message = "recommender는 필수입니다.")
//    private String recommender;
}
