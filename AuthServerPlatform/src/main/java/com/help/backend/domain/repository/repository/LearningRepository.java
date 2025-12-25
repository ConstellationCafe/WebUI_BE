package com.help.backend.domain.repository.repository;

import com.help.backend.domain.repository.entity.LearningEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LearningRepository extends JpaRepository<LearningEntity, Long> {
    List<LearningEntity> findByTeacher(String teacher);
}
