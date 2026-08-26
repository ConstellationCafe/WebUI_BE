package com.help.erpweb.domain.academy.repository;

import com.help.erpweb.domain.academy.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StudentRepository
        extends JpaRepository<Student, Integer> {

    @Query("""
        SELECT s
        FROM Student s
        JOIN FETCH s.academyClass c
        JOIN FETCH c.academy a
        WHERE a.id = :academyId
          AND c.id = :classId
    """)
    List<Student> findByAcademyIdAndClassId(
            @Param("academyId") Integer academyId,
            @Param("classId") Integer classId
    );

    @Query("""
        SELECT DISTINCT s.state
        FROM Student s
        JOIN s.academyClass c
        JOIN c.academy a
        WHERE (:academyId IS NULL OR a.id = :academyId)
          AND (:classId IS NULL OR c.id = :classId)
        ORDER BY s.state
    """)
    List<String> findDistinctStates(
            @Param("academyId") Integer academyId,
            @Param("classId") Integer classId
    );

    List<Student> findByAcademyClass_Academy_Id(
            Integer academyId
    );

    List<Student> findByAcademyClass_Id(
            Integer classId
    );

    Optional<Student> findBySk(
            String sk
    );

    @Query(
            value = """
                SELECT
                    s.s_id AS studentId,
                    s.sk AS studentSk,
                    a.a_id AS academyId,
                    a.name AS academyName,
                    c.c_id AS classId,
                    c.class AS classNumber,
                    c.state AS classState,
                    CASE
                        WHEN g.s_id IS NOT NULL THEN 'GRADUATED'
                        WHEN s.state = '재적' THEN 'ENROLLED'
                        WHEN s.state = '퇴학' THEN 'EXPELLED'
                        WHEN s.state = '자퇴' THEN 'WITHDRAWN'
                        WHEN s.state = '은퇴' THEN 'RETIRED'
                        WHEN s.state = '징계' THEN 'DISCIPLINARY'
                        ELSE NULL
                    END AS status,
                    g.graduate_at AS graduateAt,
                    sp.suspended_at AS suspendedAt,
                    sp.reason AS suspendReason
                FROM Academy.Students s
                JOIN Academy.Class c
                    ON c.c_id = s.c_id
                JOIN Academy.Academy a
                    ON a.a_id = c.a_id
                LEFT JOIN Academy.Graduates g
                    ON g.s_id = s.s_id
                LEFT JOIN Academy.Suspender sp
                    ON sp.s_id = s.s_id
                WHERE
                    (
                        :academyId IS NULL
                        OR a.a_id = :academyId
                    )
                    AND (
                        :classId IS NULL
                        OR c.c_id = :classId
                    )
                    AND (
                        :studentId IS NULL
                        OR :studentId = ''
                        OR s.sk = :studentId
                    )
                    AND (
                        :status IS NULL
                        OR :status = ''
                        OR (
                            CASE
                                WHEN g.s_id IS NOT NULL THEN 'GRADUATED'
                                WHEN s.state = '재적' THEN 'ENROLLED'
                                WHEN s.state = '퇴학' THEN 'EXPELLED'
                                WHEN s.state = '자퇴' THEN 'WITHDRAWN'
                                WHEN s.state = '은퇴' THEN 'RETIRED'
                                WHEN s.state = '징계' THEN 'DISCIPLINARY'
                                ELSE NULL
                            END
                        ) = :status
                    )
                ORDER BY
                    s.s_id DESC
                """,
            countQuery = """
                SELECT COUNT(*)
                FROM Academy.Students s
                JOIN Academy.Class c
                    ON c.c_id = s.c_id
                JOIN Academy.Academy a
                    ON a.a_id = c.a_id
                LEFT JOIN Academy.Graduates g
                    ON g.s_id = s.s_id
                LEFT JOIN Academy.Suspender sp
                    ON sp.s_id = s.s_id
                WHERE
                    (
                        :academyId IS NULL
                        OR a.a_id = :academyId
                    )
                    AND (
                        :classId IS NULL
                        OR c.c_id = :classId
                    )
                    AND (
                        :studentId IS NULL
                        OR :studentId = ''
                        OR s.sk = :studentId
                    )
                    AND (
                        :status IS NULL
                        OR :status = ''
                        OR (
                            CASE
                                WHEN g.s_id IS NOT NULL THEN 'GRADUATED'
                                WHEN s.state = '재적' THEN 'ENROLLED'
                                WHEN s.state = '퇴학' THEN 'EXPELLED'
                                WHEN s.state = '자퇴' THEN 'WITHDRAWN'
                                WHEN s.state = '은퇴' THEN 'RETIRED'
                                WHEN s.state = '징계' THEN 'DISCIPLINARY'
                                ELSE NULL
                            END
                        ) = :status
                    )
                """,
            nativeQuery = true
    )
    Page<StudentStatusProjection> findStudentStatuses(
            @Param("academyId")
            Integer academyId,
            @Param("classId")
            Integer classId,
            @Param("studentId")
            String studentId,
            @Param("status")
            String status,
            Pageable pageable
    );

    @Query(
            value = """
                SELECT
                    COUNT(*) AS totalCount,
                    SUM(
                        CASE
                            WHEN g.s_id IS NULL
                                AND s.state = '재적'
                            THEN 1
                            ELSE 0
                        END
                    ) AS enrolledCount,
                    SUM(
                        CASE
                            WHEN g.s_id IS NOT NULL
                            THEN 1
                            ELSE 0
                        END
                    ) AS graduationCount,
                    SUM(
                        CASE
                            WHEN g.s_id IS NULL
                                AND s.state = '퇴학'
                            THEN 1
                            ELSE 0
                        END
                    ) AS expulsionCount,
                    SUM(
                        CASE
                            WHEN g.s_id IS NULL
                                AND s.state = '자퇴'
                            THEN 1
                            ELSE 0
                        END
                    ) AS withdrawalCount,
                    SUM(
                        CASE
                            WHEN g.s_id IS NULL
                                AND s.state = '은퇴'
                            THEN 1
                            ELSE 0
                        END
                    ) AS retirementCount,
                    SUM(
                        CASE
                            WHEN g.s_id IS NULL
                                AND s.state = '징계'
                            THEN 1
                            ELSE 0
                        END
                    ) AS disciplinaryCount
                FROM Academy.Students s
                JOIN Academy.Class c
                    ON c.c_id = s.c_id
                JOIN Academy.Academy a
                    ON a.a_id = c.a_id
                LEFT JOIN Academy.Graduates g
                    ON g.s_id = s.s_id
                WHERE
                    (
                        :academyId IS NULL
                        OR a.a_id = :academyId
                    )
                    AND (
                        :classId IS NULL
                        OR c.c_id = :classId
                    )
                    AND (
                        :studentId IS NULL
                        OR :studentId = ''
                        OR s.sk = :studentId
                    )
                """,
            nativeQuery = true
    )
    StudentStatusSummaryProjection findStudentStatusSummary(
            @Param("academyId")
            Integer academyId,
            @Param("classId")
            Integer classId,
            @Param("studentId")
            String studentId
    );

    interface StudentStatusProjection {
        Integer getStudentId();
        String getStudentSk();
        Integer getAcademyId();
        String getAcademyName();
        Integer getClassId();
        Integer getClassNumber();
        String getClassState();
        String getStatus();
        LocalDate getGraduateAt();
        LocalDate getSuspendedAt();
        String getSuspendReason();
    }

    interface StudentStatusSummaryProjection {
        Long getTotalCount();
        Long getEnrolledCount();
        Long getGraduationCount();
        Long getExpulsionCount();
        Long getWithdrawalCount();
        Long getRetirementCount();
        Long getDisciplinaryCount();
    }
}