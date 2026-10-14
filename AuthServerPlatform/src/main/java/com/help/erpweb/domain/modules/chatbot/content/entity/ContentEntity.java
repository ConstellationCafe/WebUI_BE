package com.help.erpweb.domain.modules.chatbot.content.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "RecommendContent")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Access(AccessType.FIELD)
public class ContentEntity {
    @Id
    @Column(name = "cn_value", nullable = false, columnDefinition = "TEXT")
    private String cnValue;

    @Column(name = "recommender", nullable = false)
    // DB에는 추천인 SK가 저장된다(API의 recommenderDiscordId와 다름).
    private String recommenderSk;

    public static ContentEntity of(String cnValue, String recommenderSk) {
        return new ContentEntity(cnValue, recommenderSk);
    }
}
