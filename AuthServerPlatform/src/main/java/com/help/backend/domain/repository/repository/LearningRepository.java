package com.help.backend.domain.repository.repository;

import com.help.backend.domain.global.repository.GlobalRepository;
import com.help.backend.domain.repository.entity.LearningEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LearningRepository extends JpaRepository<LearningEntity, Long>, GlobalRepository, LearningRepositoryCustom {
    String schemaName = "ChatBot";
    String tableName = "Learning";

    @Query(value = "SELECT * FROM ChatBot.Learning WHERE teacher = :teacher",
            nativeQuery = true)
    List<LearningEntity> findByTeacher(String teacher);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        DELETE FROM ChatBot.Learning
        WHERE teacher = :teacher
        AND ln_key IN (:lnKeys)
        """, nativeQuery = true)
    int deleteByLnKey(
        @Param("teacher") String teacher,
        @Param("lnKeys") List<String> lnKeys
    );
}
