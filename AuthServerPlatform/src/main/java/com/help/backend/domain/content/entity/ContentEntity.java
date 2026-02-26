package com.help.backend.domain.content.entity;

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
    private String recommender;

    public static ContentEntity of(String cnValue, String recommender) {
        return new ContentEntity(cnValue, recommender);
    }
}
