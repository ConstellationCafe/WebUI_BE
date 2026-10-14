package com.help.erpweb.domain.modules.chatbot.music.entity;

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
    // DB에는 추천인 SK가 저장된다(API의 recommenderDiscordId와 다름).
    private String recommenderSk;

    public static MusicEntity of(String videoId, String recommenderSk) {
        return new MusicEntity(videoId, recommenderSk);
    }
}
