package com.help.erpweb.domain.notification.repository;

import com.help.erpweb.domain.notification.entity.Notification;
import com.help.erpweb.domain.notification.entity.NotificationSource;
import com.help.erpweb.domain.notification.entity.NotificationTargetType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 회원이 볼 수 있는 알림(채팅방 전체 + 본인 대상)을 ID 내림차순으로 조회한다.
     * ID는 단조 증가하므로 커서 페이지네이션의 안정적인 정렬 기준으로 쓴다.
     */
    @Query("""
        SELECT n
        FROM Notification n
        WHERE n.botId = :botId
          AND (n.targetType = :guild OR n.targetDiscordId = :discordId)
          AND n.id < :beforeId
        ORDER BY n.id DESC
    """)
    List<Notification> findVisibleBefore(
            @Param("botId") String botId,
            @Param("discordId") String discordId,
            @Param("guild") NotificationTargetType guild,
            @Param("beforeId") long beforeId,
            Pageable pageable
    );

    @Query("""
        SELECT COUNT(n)
        FROM Notification n
        WHERE n.botId = :botId
          AND (n.targetType = :guild OR n.targetDiscordId = :discordId)
          AND n.id > :afterId
    """)
    long countVisibleAfter(
            @Param("botId") String botId,
            @Param("discordId") String discordId,
            @Param("guild") NotificationTargetType guild,
            @Param("afterId") long afterId
    );

    @Query("""
        SELECT MAX(n.id)
        FROM Notification n
        WHERE n.botId = :botId
          AND (n.targetType = :guild OR n.targetDiscordId = :discordId)
    """)
    Optional<Long> findLatestVisibleId(
            @Param("botId") String botId,
            @Param("discordId") String discordId,
            @Param("guild") NotificationTargetType guild
    );

    Page<Notification> findByBotIdOrderByIdDesc(String botId, Pageable pageable);

    Optional<Notification> findByBotIdAndSourceAndSourceRefAndRequestKey(
            String botId,
            NotificationSource source,
            String sourceRef,
            String requestKey
    );

    /**
     * 멱등 키가 있는 발행 전용. 같은 키가 이미 있으면 아무것도 바꾸지 않는다(영향 행 0).
     * 같은 키의 동시 요청은 unique key 잠금으로 직렬화되므로, 호출자는 반환값으로
     * 새로 만들었는지 판단한 뒤 키로 다시 조회한다. 예외로 트랜잭션이 rollback-only가
     * 되지 않도록 INSERT 실패 대신 ON DUPLICATE KEY를 쓴다(ADR-0004).
     */
    @Modifying(flushAutomatically = true, clearAutomatically = false)
    @Query(value = """
        INSERT INTO Constellation_Network.Notification
            (bot_id, target_type, target_discord_id, category, title, body, link,
             source, source_ref, request_key, created_at)
        VALUES
            (:botId, :targetType, :targetDiscordId, :category, :title, :body, :link,
             :source, :sourceRef, :requestKey, :createdAt)
        ON DUPLICATE KEY UPDATE id = id
    """, nativeQuery = true)
    int insertIfAbsent(
            @Param("botId") String botId,
            @Param("targetType") String targetType,
            @Param("targetDiscordId") String targetDiscordId,
            @Param("category") String category,
            @Param("title") String title,
            @Param("body") String body,
            @Param("link") String link,
            @Param("source") String source,
            @Param("sourceRef") String sourceRef,
            @Param("requestKey") String requestKey,
            @Param("createdAt") LocalDateTime createdAt
    );
}
