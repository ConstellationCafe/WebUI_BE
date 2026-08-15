package com.help.erpweb.domain.academy.repository;

import com.help.erpweb.domain.academy.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentRepository
        extends JpaRepository<Student, Integer> {

    List<Student> findByAcademyClass_Academy_Id(
            Integer academyId
    );

    List<Student> findByAcademyClass_Id(
            Integer classId
    );

    Optional<Student> findBySk(String sk);
}