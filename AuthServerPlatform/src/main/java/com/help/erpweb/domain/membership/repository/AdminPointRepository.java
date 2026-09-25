package com.help.erpweb.domain.membership.repository;

import com.help.erpweb.domain.membership.dto.response.AdminPointLogResponse;
import com.help.erpweb.domain.membership.dto.response.AdminPointMemberResponse;
import com.help.erpweb.domain.membership.exception.ActiveMemberNotFoundException;
import com.help.erpweb.domain.membership.exception.PointLogConflictException;
import com.help.erpweb.domain.membership.exception.PointLogNotFoundException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Repository;

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
                            ON d.discordID = u.discordID
                        INNER JOIN Constellation_Network.CoinTable c
                            ON c.sk = u.sk
                        WHERE d.state = :state
                          AND (:discordId = '' OR u.discordID LIKE CONCAT('%', :discordId, '%'))
                        ORDER BY d.username ASC, u.discordID ASC
                        """)
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
                            ON d.discordID = u.discordID
                        INNER JOIN Constellation_Network.CoinTable c
                            ON c.sk = u.sk
                        WHERE d.state = :state
                          AND (:discordId = '' OR u.discordID LIKE CONCAT('%', :discordId, '%'))
                        """)
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
                            ON d.discordID = u.discordID
                        INNER JOIN Constellation_Network.CoinTable c
                            ON c.sk = u.sk
                        WHERE u.discordID = :discordId
                          AND d.state = :state
                        """)
                .setParameter("discordId", discordId)
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
                            ON d.discordID = u.discordID
                        WHERE u.discordID = :discordId
                          AND d.state = :state
                        ORDER BY p.at DESC, p.amount DESC
                        """)
                .setParameter("discordId", discordId)
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
                            ON d.discordID = u.discordID
                        WHERE u.discordID = :discordId
                          AND d.state = :state
                        """)
                .setParameter("discordId", discordId)
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
                            ON d.discordID = u.discordID
                        INNER JOIN Constellation_Network.CoinTable c
                            ON c.sk = u.sk
                        WHERE u.discordID = :discordId
                          AND d.state = :state
                        FOR UPDATE
                        """)
                .setParameter("discordId", discordId)
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
