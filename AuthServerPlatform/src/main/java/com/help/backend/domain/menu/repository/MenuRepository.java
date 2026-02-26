package com.help.backend.domain.menu.repository;

import com.help.backend.domain.metadata.repository.GlobalRepository;
import com.help.backend.domain.menu.entity.MenuEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuRepository extends JpaRepository<MenuEntity, Long>, GlobalRepository, MenuRepositoryCustom {
    String schemaName = "ChatBot";
    String tableName = "RecommendMenu";

    @Query(value = "SELECT * FROM ChatBot.RecommendMenu WHERE recommender = :recommender",
            nativeQuery = true)
    List<MenuEntity> findByRecommender(String recommender);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        DELETE FROM ChatBot.RecommendMenu
        WHERE recommender = :recommender
        AND mn_value IN (:mnValues)
        """, nativeQuery = true)
    int deleteByMnValue(
        @Param("recommender") String recommender,
        @Param("mnValues") List<String> mnValues
    );
}
