package com.help.erpweb.domain.modules.chatbot.learning.entity;

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
    // DB에는 교사 SK가 저장된다(API의 teacherDiscordId와 다름).
    private String teacherSk;

    public static LearningEntity of(String lnKey, String lnValue, String teacherSk) {
        return new LearningEntity(lnKey, lnValue, teacherSk);
    }
}
