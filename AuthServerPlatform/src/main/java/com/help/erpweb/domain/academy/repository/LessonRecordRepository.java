package com.help.erpweb.domain.academy.repository;

import com.help.erpweb.domain.academy.entity.LessonRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LessonRecordRepository
        extends JpaRepository<LessonRecord, Long> {

    @Query(value = """
        SELECT
            lr.lr_id AS id,
            a.name AS academyName,
            lr.class_name AS className,
            lr.subject AS subject,
            DATE(lr.education_date) AS educationDate,
            lr.education_duration AS educationDuration,
            lr.main_teacher_id AS mainTeacherId,
            lr.description AS description,
            COALESCE(JSON_LENGTH(lr.member_ids), 0) AS memberCount

        FROM Academy.LessonRecord lr

        JOIN Academy.Academy a
            ON a.a_id = lr.a_id

        WHERE
            (
                :academyId IS NULL
                OR lr.a_id = :academyId
            )

            AND (
                :className IS NULL
                OR :className = ''
                OR lr.class_name = :className
            )

            AND (
                :date IS NULL
                OR DATE(lr.education_date) = :date
            )

            AND (
                :time IS NULL
                OR :time = ''
                OR (
                    :time = '오전'
                    AND HOUR(lr.education_date) < 12
                )
                OR (
                    :time = '오후'
                    AND HOUR(lr.education_date) >= 12
                )
                OR DATE_FORMAT(
                    lr.education_date,
                    '%H:%i'
                ) = :time
            )

            AND (
                :subject IS NULL
                OR :subject = ''
                OR lr.subject = :subject
            )

            AND (
                :teacherId IS NULL
                OR :teacherId = ''
                OR lr.main_teacher_id = :teacherId
            )

        ORDER BY lr.education_date DESC
        """, nativeQuery = true)
    List<LessonRecordSummaryProjection> findLessonRecordSummaries(
            @Param("academyId")
            Integer academyId,

            @Param("className")
            String className,

            @Param("date")
            LocalDate date,

            @Param("time")
            String time,

            @Param("subject")
            String subject,

            @Param("teacherId")
            String teacherId
    );

    interface LessonRecordSummaryProjection {

        Long getId();

        String getAcademyName();

        String getClassName();

        String getSubject();

        LocalDate getEducationDate();

        Integer getEducationDuration();

        String getMainTeacherId();

        String getDescription();

        Integer getMemberCount();
    }
}