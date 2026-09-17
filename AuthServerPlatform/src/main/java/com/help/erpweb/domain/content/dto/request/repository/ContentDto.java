package com.help.erpweb.domain.content.dto.request.repository;

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

    /*
     * API에서는 Discord ID를 사용한다.
     * 실제 DB에는 Stored Procedure에서 SK로 변환하여 저장한다.
     */
    @NotBlank(message = "recommender는 필수입니다.")
    private String recommender;
}
