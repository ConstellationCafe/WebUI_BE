package com.help.erpweb.domain.notification.repository;

import com.help.erpweb.domain.notification.entity.NotificationReadCursor;
import com.help.erpweb.domain.notification.entity.NotificationReadCursorId;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationReadCursorRepository
        extends JpaRepository<NotificationReadCursor, NotificationReadCursorId> {

    @Query("""
        SELECT c.lastReadId
        FROM NotificationReadCursor c
        WHERE c.botId = :botId
          AND c.discordId = :discordId
    """)
    Optional<Long> findLastReadId(
            @Param("botId") String botId,
            @Param("discordId") String discordId
    );

    /**
     * 읽음 위치는 앞으로만 움직인다. 여러 탭에서 늦게 도착한 요청이 더 작은 값을 보내도
     * GREATEST로 되돌아가지 않으며, 같은 요청을 다시 보내도 결과가 같다(멱등).
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
        INSERT INTO Constellation_Network.NotificationReadCursor
            (bot_id, discord_id, last_read_id, updated_at)
        VALUES
            (:botId, :discordId, :lastReadId, :updatedAt)
        ON DUPLICATE KEY UPDATE
            last_read_id = GREATEST(last_read_id, :lastReadId),
            updated_at = :updatedAt
    """, nativeQuery = true)
    int advance(
            @Param("botId") String botId,
            @Param("discordId") String discordId,
            @Param("lastReadId") long lastReadId,
            @Param("updatedAt") LocalDateTime updatedAt
    );
}
