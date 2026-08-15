package com.help.erpweb.domain.academy.repository;

import com.help.erpweb.domain.academy.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeacherRepository
        extends JpaRepository<Teacher, Integer> {

    List<Teacher> findByAcademyClass_Academy_Id(
            Integer academyId
    );

    List<Teacher> findByAcademyClass_Id(
            Integer classId
    );
}