package com.help.erpweb.domain.content.repository;

import com.help.erpweb.domain.content.entity.ContentEntity;
import com.help.erpweb.domain.metadata.repository.GlobalRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContentRepository extends JpaRepository<ContentEntity, Long>, GlobalRepository, ContentRepositoryCustom {
    String schemaName = "ChatBot";
    String tableName = "RecommendContent";

    @Query(value = "SELECT * FROM ChatBot.RecommendContent WHERE recommender = :recommender",
            nativeQuery = true)
    List<ContentEntity> findByRecommender(String recommender);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        DELETE FROM ChatBot.RecommendContent
        WHERE recommender = :recommender
        AND cn_value IN (:cnValues)
        """, nativeQuery = true)
    int deleteByMnValue(
        @Param("recommender") String recommender,
        @Param("cnValues") List<String> cnValues
    );
}
