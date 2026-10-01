package com.help.erpweb.domain.modules.chatbot.content.repository;

import com.help.erpweb.domain.modules.chatbot.content.entity.ContentEntity;
import com.help.erpweb.domain.metadata.repository.GlobalRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContentRepository
        extends JpaRepository<ContentEntity, String>, GlobalRepository, ContentRepositoryCustom {
    String schemaName = "ChatBot";
    String tableName = "RecommendContent";

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
