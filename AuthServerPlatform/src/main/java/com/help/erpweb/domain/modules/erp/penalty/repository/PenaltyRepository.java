package com.help.erpweb.domain.modules.erp.penalty.repository;

import com.help.erpweb.domain.modules.erp.penalty.dto.request.PenaltySort;
import com.help.erpweb.domain.modules.erp.penalty.dto.response.PenaltyMemberRankResponse;
import com.help.erpweb.domain.modules.erp.penalty.entity.PenaltyLog;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
public class PenaltyRepository {
    private static final String ACTIVE_MEMBER = "재적";

    @PersistenceContext(unitName = "constellation")
    private EntityManager entityManager;

    public PenaltyMember findActiveMember(String botId, String discordId, boolean lock) {
        String sql = """
                SELECT u.discordID, d.username, d.state, u.sk
                FROM Constellation_Network.Users u
                JOIN Constellation_Network.DiscordUsers d
                  ON d.bot_id = u.bot_id AND d.discordID = u.discordID
                WHERE u.bot_id = :botId AND u.discordID = :discordId AND d.state = :state
                """ + (lock ? " FOR UPDATE" : "");
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("botId", botId)
                .setParameter("discordId", discordId)
                .setParameter("state", ACTIVE_MEMBER)
                .setMaxResults(1)
                .getResultList();
        if (rows.isEmpty()) {
            return null;
        }
        Object[] row = rows.get(0);
        return new PenaltyMember(row[0].toString(), row[1] == null ? "" : row[1].toString(),
                row[2].toString(), row[3].toString());
    }

    /** MySQL's unique key serializes concurrent retries even when they name different members. */
    public void insertIfAbsent(String botId, PenaltyMember member, String requestId,
                               String channelId, String channelName, String reason, int score,
                               String issuer, Instant occurredAt, Instant requestedAt, Instant now) {
        entityManager.createNativeQuery("""
                        INSERT INTO Constellation_Network.PenaltyLog
                          (bot_id, sk, target_discord_id, target_username, channel_id, channel_name,
                           reason, score, issuer_discord_id, occurred_at, requested_occurred_at,
                           created_at, status, request_id)
                        VALUES (:botId, :sk, :targetId, :username, :channelId, :channelName,
                                :reason, :score, :issuer, :occurredAt, :requestedAt,
                                :createdAt, 'ACTIVE', :requestId)
                        ON DUPLICATE KEY UPDATE id = id
                        """)
                .setParameter("botId", botId)
                .setParameter("sk", member.sk())
                .setParameter("targetId", member.discordId())
                .setParameter("username", member.username())
                .setParameter("channelId", channelId)
                .setParameter("channelName", channelName)
                .setParameter("reason", reason)
                .setParameter("score", score)
                .setParameter("issuer", issuer)
                .setParameter("occurredAt", utc(occurredAt))
                .setParameter("requestedAt", requestedAt == null ? null : utc(requestedAt))
                .setParameter("createdAt", utc(now))
                .setParameter("requestId", requestId)
                .executeUpdate();
    }

    public PenaltyLog findByRequestId(String botId, String requestId) {
        @SuppressWarnings("unchecked")
        List<PenaltyLog> rows = entityManager.createNativeQuery("""
                        SELECT * FROM Constellation_Network.PenaltyLog
                        WHERE bot_id = :botId AND request_id = :requestId
                        """, PenaltyLog.class)
                .setParameter("botId", botId).setParameter("requestId", requestId)
                .getResultList();
        return rows.isEmpty() ? null : rows.get(0);
    }

    public PenaltyLog lockById(String botId, long penaltyId) {
        @SuppressWarnings("unchecked")
        List<PenaltyLog> rows = entityManager.createNativeQuery("""
                        SELECT * FROM Constellation_Network.PenaltyLog
                        WHERE bot_id = :botId AND id = :id FOR UPDATE
                        """, PenaltyLog.class)
                .setParameter("botId", botId).setParameter("id", penaltyId)
                .getResultList();
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void flush() {
        entityManager.flush();
    }

    public long countHistory(String botId, String channelId, String discordId) {
        return ((Number) entityManager.createNativeQuery("""
                        SELECT COUNT(*) FROM Constellation_Network.PenaltyLog
                        WHERE bot_id = :botId
                          AND (:channelId IS NULL OR channel_id = :channelId)
                          AND (:discordId IS NULL OR target_discord_id = :discordId)
                        """)
                .setParameter("botId", botId).setParameter("channelId", channelId)
                .setParameter("discordId", discordId).getSingleResult()).longValue();
    }

    public List<PenaltyLog> findHistory(String botId, String channelId, String discordId,
                                        PenaltySort sort, int page, int size) {
        long offset = ((long) page - 1) * size;
        if (offset > Integer.MAX_VALUE) {
            return List.of();
        }
        String order = sort == PenaltySort.OCCURRED_AT_ASC
                ? " ORDER BY occurred_at ASC, id ASC" : " ORDER BY occurred_at DESC, id DESC";
        @SuppressWarnings("unchecked")
        List<PenaltyLog> rows = entityManager.createNativeQuery("""
                        SELECT * FROM Constellation_Network.PenaltyLog
                        WHERE bot_id = :botId
                          AND (:channelId IS NULL OR channel_id = :channelId)
                          AND (:discordId IS NULL OR target_discord_id = :discordId)
                        """ + order, PenaltyLog.class)
                .setParameter("botId", botId).setParameter("channelId", channelId)
                .setParameter("discordId", discordId)
                .setFirstResult((int) offset).setMaxResults(size).getResultList();
        return rows;
    }

    public Map<String, Long> findCumulativeScores(String botId, List<String> discordIds,
                                                   Instant since, Instant now) {
        if (discordIds.isEmpty()) {
            return Map.of();
        }
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                        SELECT target_discord_id, SUM(score)
                        FROM Constellation_Network.PenaltyLog
                        WHERE bot_id = :botId AND status = 'ACTIVE'
                          AND occurred_at >= :since AND occurred_at < :now
                          AND target_discord_id IN (:ids)
                        GROUP BY target_discord_id
                        """)
                .setParameter("botId", botId).setParameter("since", utc(since))
                .setParameter("now", utc(now)).setParameter("ids", discordIds)
                .getResultList();
        Map<String, Long> scores = new HashMap<>();
        for (Object[] row : rows) {
            scores.put(row[0].toString(), ((Number) row[1]).longValue());
        }
        return scores;
    }

    public long countRankedMembers(String botId, String search, Instant since, Instant now) {
        return ((Number) entityManager.createNativeQuery("""
                        SELECT COUNT(DISTINCT p.target_discord_id)
                        FROM Constellation_Network.PenaltyLog p
                        JOIN Constellation_Network.Users u
                          ON u.bot_id = p.bot_id AND u.sk = p.sk
                        JOIN Constellation_Network.DiscordUsers d
                          ON d.bot_id = u.bot_id AND d.discordID = u.discordID
                        WHERE p.bot_id = :botId AND p.status = 'ACTIVE'
                          AND p.occurred_at >= :since AND p.occurred_at < :now
                          AND d.state = :state AND p.target_discord_id LIKE CONCAT('%', :search, '%')
                        """)
                .setParameter("botId", botId).setParameter("since", utc(since))
                .setParameter("now", utc(now)).setParameter("state", ACTIVE_MEMBER)
                .setParameter("search", search == null ? "" : search).getSingleResult()).longValue();
    }

    public List<PenaltyMemberRankResponse> findRankedMembers(String botId, String search,
                                                              Instant since, Instant now,
                                                              int page, int size) {
        long offset = ((long) page - 1) * size;
        if (offset > Integer.MAX_VALUE) {
            return List.of();
        }
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                        SELECT p.target_discord_id, d.username, SUM(p.score), COUNT(*), MAX(p.occurred_at)
                        FROM Constellation_Network.PenaltyLog p
                        JOIN Constellation_Network.Users u
                          ON u.bot_id = p.bot_id AND u.sk = p.sk
                        JOIN Constellation_Network.DiscordUsers d
                          ON d.bot_id = u.bot_id AND d.discordID = u.discordID
                        WHERE p.bot_id = :botId AND p.status = 'ACTIVE'
                          AND p.occurred_at >= :since AND p.occurred_at < :now
                          AND d.state = :state AND p.target_discord_id LIKE CONCAT('%', :search, '%')
                        GROUP BY p.target_discord_id, d.username
                        ORDER BY SUM(p.score) DESC, p.target_discord_id ASC
                        """)
                .setParameter("botId", botId).setParameter("since", utc(since))
                .setParameter("now", utc(now)).setParameter("state", ACTIVE_MEMBER)
                .setParameter("search", search == null ? "" : search)
                .setFirstResult((int) offset).setMaxResults(size).getResultList();
        return rows.stream().map(row -> new PenaltyMemberRankResponse(
                row[0].toString(), row[1] == null ? "" : row[1].toString(),
                ((Number) row[2]).longValue(), ((Number) row[3]).longValue(),
                timestamp(row[4]).toInstant(ZoneOffset.UTC)
        )).toList();
    }

    private static LocalDateTime utc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static LocalDateTime timestamp(Object value) {
        return value instanceof Timestamp timestamp ? timestamp.toLocalDateTime() : (LocalDateTime) value;
    }
}
