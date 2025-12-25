package com.help.backend.domain.repository.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "learning")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearningEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ln_key", nullable = false)
    private String lnKey;

    @Column(name = "ln_value", nullable = false, columnDefinition = "TEXT")
    private String lnValue;

    @Column(name = "teacher", nullable = false)
    private String teacher;
}
