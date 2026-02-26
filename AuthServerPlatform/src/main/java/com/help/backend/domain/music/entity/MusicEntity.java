package com.help.backend.domain.music.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "RecommendMusic")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Access(AccessType.FIELD)
public class MusicEntity {
    @Id
    @Column(name = "video_id", nullable = false, columnDefinition = "TEXT")
    private String videoId;

    @Column(name = "recommender", nullable = false)
    private String recommender;

    public static MusicEntity of(String videoId, String recommender) {
        return new MusicEntity(videoId, recommender);
    }
}
