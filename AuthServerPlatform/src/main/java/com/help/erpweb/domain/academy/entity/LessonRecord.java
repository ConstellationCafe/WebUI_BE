package com.help.erpweb.domain.academy.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "LessonRecord",
        catalog = "Academy"
)
public class LessonRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lr_id")
    private Long id;

    @Column(name = "a_id", nullable = false)
    private Integer academyId;

    @Column(name = "class_name", nullable = false, length = 100)
    private String className;

    @Column(name = "subject", nullable = false, length = 100)
    private String subject;

    @Column(name = "education_date", nullable = false)
    private LocalDateTime educationDate;

    @Column(name = "education_duration", nullable = false)
    private Integer educationDuration;

    @Column(name = "main_teacher_id", nullable = false)
    private String mainTeacherId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "co_teacher_ids", columnDefinition = "json")
    private JsonNode coTeacherIds;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "member_ids", columnDefinition = "json")
    private JsonNode memberIds;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    public LessonRecord(
            Integer academyId,
            String className,
            String subject,
            LocalDateTime educationDate,
            Integer educationDuration,
            String mainTeacherId,
            JsonNode coTeacherIds,
            JsonNode memberIds,
            String description
    ) {
        this.academyId = academyId;
        this.className = className;
        this.subject = subject;
        this.educationDate = educationDate;
        this.educationDuration = educationDuration;
        this.mainTeacherId = mainTeacherId;
        this.coTeacherIds = coTeacherIds;
        this.memberIds = memberIds;
        this.description = description;
    }
}