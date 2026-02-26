package com.help.backend.domain.menu.entity;

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
    private String recommender;

    public static MenuEntity of(String mnValue, String recommender) {
        return new MenuEntity(mnValue, recommender);
    }
}
