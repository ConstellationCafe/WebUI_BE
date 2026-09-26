package com.help.erpweb.domain.modules.erp.point.repository;

import com.help.erpweb.domain.modules.erp.point.dto.response.AdminPointLogResponse;
import com.help.erpweb.domain.modules.erp.point.dto.response.AdminPointMemberResponse;
import com.help.erpweb.domain.modules.erp.point.exception.ActiveMemberNotFoundException;
import com.help.erpweb.domain.modules.erp.point.exception.PointLogConflictException;
import com.help.erpweb.domain.modules.erp.point.exception.PointLogNotFoundException;
import com.help.global.guild.GuildContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * ADR-0001: admin은 항상 현재 요청의 GuildContext(botId)로 스코프된다.
 * 즉 이 리포지토리의 모든 조회/변경은 "관리자가 현재 보고 있는 채팅방(bot)"
 * 범위 안에서만 유효하며, 다른 봇에 속한 회원 데이터는 조회/변경할 수 없다.
 * <p>
 * 전제: {@code Constellation_Network.Users}와 {@code DiscordUsers}에
 * {@code bot_id} 컬럼이 추가되어 있다(search_sk가 이미 botId를 반영해
 * bot별로 다른 sk를 부여하므로, discordID만으로는 더 이상 행이 유일하지 않음).
 * sk 자체는 이미 bot에 스코프된 값이므로, sk로만 동작하는 하위 쿼리
 * (findCoin/updateCoin/insertLog/lockLog/updateLog/deleteLog)는 botId 필터가
 * 필요 없다.
 */
@Repository
public class AdminPointRepository {
    private static final String ACTIVE_STATE = "재적";

    @PersistenceContext(unitName = "constellation")
    private EntityManager entityManager;

    public List<AdminPointMemberResponse> findActiveMembers(
            String discordId,
            int offset,
            int size
    ) {
        String search = normalizeSearch(discordId);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                        SELECT u.discordID, d.username, d.state, c.coin
                        FROM Constellation_Network.Users u
                        INNER JOIN Constellation_Network.DiscordUsers d
                            ON d.discordID = u.discordID AND d.bot_id = u.bot_id
                        INNER JOIN Constellation_Network.CoinTable c
                            ON c.sk = u.sk
                        WHERE u.bot_id = :botId
                          AND d.state = :state
                          AND (:discordId = '' OR u.discordID LIKE CONCAT('%', :discordId, '%'))
                        ORDER BY d.username ASC, u.discordID ASC
                        """)
                .setParameter("botId", GuildContext.requireBotId())
                .setParameter("state", ACTIVE_STATE)
                .setParameter("discordId", search)
                .setFirstResult(offset)
                .setMaxResults(size)
                .getResultList();

        return rows.stream().map(this::toMemberResponse).toList();
    }

    public long countActiveMembers(String discordId) {
        String search = normalizeSearch(discordId);
        Number count = (Number) entityManager.createNativeQuery("""
                        SELECT COUNT(*)
                        FROM Constellation_Network.Users u
                        INNER JOIN Constellation_Network.DiscordUsers d
                            ON d.discordID = u.discordID AND d.bot_id = u.bot_id
                        INNER JOIN Constellation_Network.CoinTable c
                            ON c.sk = u.sk
                        WHERE u.bot_id = :botId
                          AND d.state = :state
                          AND (:discordId = '' OR u.discordID LIKE CONCAT('%', :discordId, '%'))
                        """)
                .setParameter("botId", GuildContext.requireBotId())
                .setParameter("state", ACTIVE_STATE)
                .setParameter("discordId", search)
                .getSingleResult();
        return count.longValue();
    }

    public AdminPointMemberResponse findActiveMember(String discordId) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                        SELECT u.discordID, d.username, d.state, c.coin
                        FROM Constellation_Network.Users u
                        INNER JOIN Constellation_Network.DiscordUsers d
                            ON d.discordID = u.discordID AND d.bot_id = u.bot_id
                        INNER JOIN Constellation_Network.CoinTable c
                            ON c.sk = u.sk
                        WHERE u.discordID = :discordId
                          AND u.bot_id = :botId
                          AND d.state = :state
                        """)
                .setParameter("discordId", discordId)
                .setParameter("botId", GuildContext.requireBotId())
                .setParameter("state", ACTIVE_STATE)
                .setMaxResults(1)
                .getResultList();
        if (rows.isEmpty()) {
            throw new ActiveMemberNotFoundException();
        }
        return toMemberResponse(rows.get(0));
    }

    public List<AdminPointLogResponse> findLogs(String discordId, int offset, int size) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                        SELECT p.amount, p.at, p.description
                        FROM Constellation_Network.PayLog p
                        INNER JOIN Constellation_Network.Users u ON u.sk = p.sk
                        INNER JOIN Constellation_Network.DiscordUsers d
                            ON d.discordID = u.discordID AND d.bot_id = u.bot_id
                        WHERE u.discordID = :discordId
                          AND u.bot_id = :botId
                          AND d.state = :state
                        ORDER BY p.at DESC, p.amount DESC
                        """)
                .setParameter("discordId", discordId)
                .setParameter("botId", GuildContext.requireBotId())
                .setParameter("state", ACTIVE_STATE)
                .setFirstResult(offset)
                .setMaxResults(size)
                .getResultList();
        return rows.stream().map(this::toLogResponse).toList();
    }

    public long countLogs(String discordId) {
        Number count = (Number) entityManager.createNativeQuery("""
                        SELECT COUNT(*)
                        FROM Constellation_Network.PayLog p
                        INNER JOIN Constellation_Network.Users u ON u.sk = p.sk
                        INNER JOIN Constellation_Network.DiscordUsers d
                            ON d.discordID = u.discordID AND d.bot_id = u.bot_id
                        WHERE u.discordID = :discordId
                          AND u.bot_id = :botId
                          AND d.state = :state
                        """)
                .setParameter("discordId", discordId)
                .setParameter("botId", GuildContext.requireBotId())
                .setParameter("state", ACTIVE_STATE)
                .getSingleResult();
        return count.longValue();
    }

    public String lockActiveMemberAndGetSk(String discordId) {
        @SuppressWarnings("unchecked")
        List<String> rows = entityManager.createNativeQuery("""
                        SELECT u.sk
                        FROM Constellation_Network.Users u
                        INNER JOIN Constellation_Network.DiscordUsers d
                            ON d.discordID = u.discordID AND d.bot_id = u.bot_id
                        INNER JOIN Constellation_Network.CoinTable c
                            ON c.sk = u.sk
                        WHERE u.discordID = :discordId
                          AND u.bot_id = :botId
                          AND d.state = :state
                        FOR UPDATE
                        """)
                .setParameter("discordId", discordId)
                .setParameter("botId", GuildContext.requireBotId())
                .setParameter("state", ACTIVE_STATE)
                .setMaxResults(1)
                .getResultList();
        if (rows.isEmpty()) {
            throw new ActiveMemberNotFoundException();
        }
        return rows.get(0);
    }

    public int findCoin(String sk) {
        Number coin = (Number) entityManager.createNativeQuery("""
                        SELECT coin
                        FROM Constellation_Network.CoinTable
                        WHERE sk = :sk
                        """)
                .setParameter("sk", sk)
                .getSingleResult();
        return coin.intValue();
    }

    public void updateCoin(String sk, int coin) {
        entityManager.createNativeQuery("""
                        UPDATE Constellation_Network.CoinTable
                        SET coin = :coin
                        WHERE sk = :sk
                        """)
                .setParameter("coin", coin)
                .setParameter("sk", sk)
                .executeUpdate();
    }

    public void insertLog(String sk, int amount, LocalDateTime at, String description) {
        entityManager.createNativeQuery("""
                        INSERT INTO Constellation_Network.PayLog (sk, amount, at, description)
                        VALUES (:sk, :amount, :at, :description)
                        """)
                .setParameter("sk", sk)
                .setParameter("amount", amount)
                .setParameter("at", at)
                .setParameter("description", description)
                .executeUpdate();
    }

    public AdminPointLogResponse lockLog(String sk, int amount, LocalDateTime at) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                        SELECT amount, at, description
                        FROM Constellation_Network.PayLog
                        WHERE sk = :sk AND amount = :amount AND at = :at
                        FOR UPDATE
                        """)
                .setParameter("sk", sk)
                .setParameter("amount", amount)
                .setParameter("at", at)
                .getResultList();
        if (rows.isEmpty()) {
            throw new PointLogNotFoundException();
        }
        return toLogResponse(rows.get(0));
    }

    public void updateLog(
            String sk,
            int originalAmount,
            LocalDateTime at,
            int amount,
            String description
    ) {
        int changed = entityManager.createNativeQuery("""
                        UPDATE Constellation_Network.PayLog
                        SET amount = :amount, description = :description
                        WHERE sk = :sk AND amount = :originalAmount AND at = :at
                        """)
                .setParameter("amount", amount)
                .setParameter("description", description)
                .setParameter("sk", sk)
                .setParameter("originalAmount", originalAmount)
                .setParameter("at", at)
                .executeUpdate();
        if (changed != 1) {
            throw new PointLogConflictException();
        }
    }

    public void deleteLog(String sk, int amount, LocalDateTime at) {
        int changed = entityManager.createNativeQuery("""
                        DELETE FROM Constellation_Network.PayLog
                        WHERE sk = :sk AND amount = :amount AND at = :at
                        """)
                .setParameter("sk", sk)
                .setParameter("amount", amount)
                .setParameter("at", at)
                .executeUpdate();
        if (changed != 1) {
            throw new PointLogConflictException();
        }
    }

    private String normalizeSearch(String discordId) {
        return discordId == null ? "" : discordId.trim();
    }

    private AdminPointMemberResponse toMemberResponse(Object[] row) {
        return new AdminPointMemberResponse(
                row[0].toString(),
                row[1] == null ? "" : row[1].toString(),
                row[2].toString(),
                ((Number) row[3]).intValue()
        );
    }

    private AdminPointLogResponse toLogResponse(Object[] row) {
        LocalDateTime at = row[1] instanceof Timestamp timestamp
                ? timestamp.toLocalDateTime()
                : (LocalDateTime) row[1];
        return new AdminPointLogResponse(
                ((Number) row[0]).intValue(),
                at,
                row[2] == null ? "" : row[2].toString()
        );
    }
}
