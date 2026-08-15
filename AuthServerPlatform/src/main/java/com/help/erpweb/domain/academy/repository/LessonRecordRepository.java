package com.help.erpweb.domain.academy.repository;

import com.help.erpweb.domain.academy.entity.LessonRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LessonRecordRepository
        extends JpaRepository<LessonRecord, Long> {

    List<LessonRecord> findByAcademy_Id(
            Integer academyId
    );

    List<LessonRecord> findByAcademyClass_Id(
            Integer classId
    );
}