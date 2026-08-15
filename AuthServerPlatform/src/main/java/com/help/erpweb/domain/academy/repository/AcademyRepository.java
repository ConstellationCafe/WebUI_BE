package com.help.erpweb.domain.academy.repository;

import com.help.erpweb.domain.academy.entity.Academy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcademyRepository
        extends JpaRepository<Academy, Integer> {
}