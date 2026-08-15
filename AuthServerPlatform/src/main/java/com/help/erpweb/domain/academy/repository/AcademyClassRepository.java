package com.help.erpweb.domain.academy.repository;

import com.help.erpweb.domain.academy.entity.AcademyClass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AcademyClassRepository
        extends JpaRepository<AcademyClass, Integer> {

    List<AcademyClass> findByAcademy_Id(Integer academyId);

    Optional<AcademyClass> findByAcademy_IdAndClassNumber(
            Integer academyId,
            Integer classNumber
    );
}