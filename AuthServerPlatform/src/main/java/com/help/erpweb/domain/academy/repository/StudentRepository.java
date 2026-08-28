package com.help.erpweb.domain.academy.repository;

import com.help.erpweb.domain.academy.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query(
            value = """
                SELECT s
                FROM Student s
                JOIN FETCH s.academyClass c
                JOIN FETCH c.academy a
                WHERE (:academyId IS NULL OR a.id = :academyId)
                  AND (:classId IS NULL OR c.id = :classId)
                  AND (:academyMemberId IS NULL OR s.sk = :academyMemberId)
                  AND (:status IS NULL OR s.state = :status)
                ORDER BY s.createAt DESC, s.id DESC
            """,
            countQuery = """
                SELECT COUNT(s)
                FROM Student s
                JOIN s.academyClass c
                JOIN c.academy a
                WHERE (:academyId IS NULL OR a.id = :academyId)
                  AND (:classId IS NULL OR c.id = :classId)
                  AND (:academyMemberId IS NULL OR s.sk = :academyMemberId)
                  AND (:status IS NULL OR s.state = :status)
            """
    )
    Page<Student> findStatusPage(
            @Param("academyId") Integer academyId,
            @Param("classId") Integer classId,
            @Param("academyMemberId") String academyMemberId,
            @Param("status") String status,
            Pageable pageable
    );

    @Query("""
        SELECT COUNT(s)
        FROM Student s
        JOIN s.academyClass c
        JOIN c.academy a
        WHERE (:academyId IS NULL OR a.id = :academyId)
          AND (:classId IS NULL OR c.id = :classId)
          AND (:academyMemberId IS NULL OR s.sk = :academyMemberId)
          AND (:status IS NULL OR s.state = :status)
    """)
    long countByStatusCondition(
            @Param("academyId") Integer academyId,
            @Param("classId") Integer classId,
            @Param("academyMemberId") String academyMemberId,
            @Param("status") String status
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
}