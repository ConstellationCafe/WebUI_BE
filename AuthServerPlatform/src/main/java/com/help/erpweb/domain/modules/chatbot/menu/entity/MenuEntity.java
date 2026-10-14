package com.help.erpweb.domain.modules.chatbot.menu.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "RecommendMenu")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Access(AccessType.FIELD)
public class MenuEntity {
    @Id
    @Column(name = "mn_value", nullable = false, columnDefinition = "TEXT")
    private String mnValue;

    @Column(name = "recommender", nullable = false)
    // DB에는 추천인 SK가 저장된다(API의 recommenderDiscordId와 다름).
    private String recommenderSk;

    public static MenuEntity of(String mnValue, String recommenderSk) {
        return new MenuEntity(mnValue, recommenderSk);
    }
}
