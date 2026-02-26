package com.help.backend.domain.music.dto.request.repository;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MusicDto {
    @NotBlank(message = "videoId는 필수입니다.")
    private String videoId;

//    @NotBlank(message = "recommender는 필수입니다.")
//    private String recommender;
}
