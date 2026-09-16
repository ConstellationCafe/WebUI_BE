package com.help.erpweb.domain.music.dto.request.repository;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class MusicDto {
    @NotBlank(message = "videoId는 필수입니다.")
    private String videoId;

    @NotBlank(message = "recommender는 필수입니다.")
    private String recommender;

    private String recommenderDiscordId;
}
