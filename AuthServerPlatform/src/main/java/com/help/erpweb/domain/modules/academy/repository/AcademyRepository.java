package com.help.erpweb.domain.modules.academy.repository;

import com.help.erpweb.domain.modules.academy.entity.Academy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AcademyRepository
        extends JpaRepository<Academy, Integer> {
    List<Academy> findAllByIdIn(List<Integer> academyIds);
}
