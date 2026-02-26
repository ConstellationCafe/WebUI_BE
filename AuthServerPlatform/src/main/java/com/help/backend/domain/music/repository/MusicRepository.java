package com.help.backend.domain.music.repository;

import com.help.backend.domain.metadata.repository.GlobalRepository;
import com.help.backend.domain.music.entity.MusicEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MusicRepository extends JpaRepository<MusicEntity, Long>, GlobalRepository, MusicRepositoryCustom {
    String schemaName = "ChatBot";
    String tableName = "RecommendMusic";

    @Query(value = "SELECT * FROM ChatBot.RecommendMusic WHERE recommender = :recommender",
            nativeQuery = true)
    List<MusicEntity> findByRecommender(String recommender);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        DELETE FROM ChatBot.RecommendMusic
        WHERE recommender = :recommender
        AND video_id IN (:videoIds)
        """, nativeQuery = true)
    int deleteByMnValue(
        @Param("recommender") String recommender,
        @Param("videoIds") List<String> videoIds
    );
}
