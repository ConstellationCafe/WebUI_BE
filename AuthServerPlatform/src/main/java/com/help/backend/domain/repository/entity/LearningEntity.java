package com.help.backend.domain.repository.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Learning")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Access(AccessType.FIELD)
public class LearningEntity {
    @Id
    @Column(name = "ln_key")
    private String lnKey;

    @Column(name = "ln_value", nullable = false, columnDefinition = "TEXT")
    private String lnValue;

    @Column(name = "teacher", nullable = false)
    private String teacher;

    public static LearningEntity of(String lnKey, String lnValue, String teacher) {
        return new LearningEntity(lnKey, lnValue, teacher);
    }
}
