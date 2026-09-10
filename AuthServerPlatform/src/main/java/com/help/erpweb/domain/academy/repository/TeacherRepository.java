package com.help.erpweb.domain.academy.repository;

import com.help.erpweb.domain.academy.entity.Teacher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Integer> {
    List<Teacher> findByAcademyClass_Academy_Id(
            Integer academyId
    );

    @Query("""
        SELECT t
        FROM Teacher t
        JOIN FETCH t.academyClass c
        JOIN c.academy a
        WHERE a.id = :academyId
    """)
    List<Teacher> findByAcademyIdWithClass(
            @Param("academyId") Integer academyId
    );

    List<Teacher> findByAcademyClass_Id(
            Integer classId
    );

    @Query("""
        SELECT DISTINCT t.state
        FROM Teacher t
        JOIN t.academyClass c
        JOIN c.academy a
        WHERE (:academyId IS NULL OR a.id = :academyId)
          AND (:classId IS NULL OR c.id = :classId)
        ORDER BY t.state
    """)
    List<String> findDistinctStates(
            @Param("academyId") Integer academyId,
            @Param("classId") Integer classId
    );

    @Query(
            value = """
                SELECT t
                FROM Teacher t
                JOIN FETCH t.academyClass c
                JOIN FETCH c.academy a
                WHERE (:academyId IS NULL OR a.id = :academyId)
                  AND (:classId IS NULL OR c.id = :classId)
                  AND (:academyMemberId IS NULL OR t.sk = :academyMemberId)
                  AND (:status IS NULL OR t.state = :status)
                ORDER BY t.createAt DESC, t.id DESC
            """,
            countQuery = """
                SELECT COUNT(t)
                FROM Teacher t
                JOIN t.academyClass c
                JOIN c.academy a
                WHERE (:academyId IS NULL OR a.id = :academyId)
                  AND (:classId IS NULL OR c.id = :classId)
                  AND (:academyMemberId IS NULL OR t.sk = :academyMemberId)
                  AND (:status IS NULL OR t.state = :status)
            """
    )
    Page<Teacher> findStatusPage(
            @Param("academyId") Integer academyId,
            @Param("classId") Integer classId,
            @Param("academyMemberId") String academyMemberId,
            @Param("status") String status,
            Pageable pageable
    );

    @Query("""
        SELECT COUNT(t)
        FROM Teacher t
        JOIN t.academyClass c
        JOIN c.academy a
        WHERE (:academyId IS NULL OR a.id = :academyId)
          AND (:classId IS NULL OR c.id = :classId)
          AND (:academyMemberId IS NULL OR t.sk = :academyMemberId)
          AND (:status IS NULL OR t.state = :status)
    """)
    long countByStatusCondition(
            @Param("academyId") Integer academyId,
            @Param("classId") Integer classId,
            @Param("academyMemberId") String academyMemberId,
            @Param("status") String status
    );

    Optional<Teacher> findBySk(
            String sk
    );
}