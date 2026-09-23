package com.help.erpweb.domain.membership.repository;

import com.help.global.data.MembershipID;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class MembershipRepository {
    @PersistenceContext(unitName = "constellation")
    private EntityManager entityManager;

    public String findSkByDiscordId(String discordId) {
        return (String) entityManager
                .createNativeQuery(
                        "SELECT Constellation_Network.search_sk(:cardType, :membershipId)"
                )
                .setParameter("cardType", MembershipID.discord.name())
                .setParameter("membershipId", discordId)
                .getSingleResult();
    }

    public String findDiscordIdBySk(String sk) {
        return (String) entityManager
                .createNativeQuery("""
                SELECT discordID
                FROM Constellation_Network.Users
                WHERE sk = :sk
                """)
                .setParameter("sk", sk)
                .getSingleResult();
    }

    public Map<String, String> findDiscordIdsBySk(List<String> sks) {
        if (sks == null || sks.isEmpty()) {
            return Map.of();
        }

        List<Object[]> results = entityManager
                .createNativeQuery("""
                        SELECT sk, discordID
                        FROM Constellation_Network.Users
                        WHERE sk IN (:sks)
                        """)
                .setParameter("sks", sks)
                .getResultList();

        Map<String, String> result = new HashMap<>();
        for (Object[] row : results) {
            if (row[0] == null || row[1] == null) {
                continue;
            }
            result.put(row[0].toString(), row[1].toString());
        }
        return result;
    }

    public List<Object[]> findActivePointMembers(String discordId, long offset, int size) {
        String searchClause = discordId == null ? "" : " AND U.discordID = :discordId";
        Query query = entityManager.createNativeQuery("""
                SELECT U.discordID, D.username, U.sk, COALESCE(C.coin, 0) AS coin
                FROM Constellation_Network.Users U
                JOIN Constellation_Network.DiscordUsers D ON D.discordID = U.discordID
                LEFT JOIN Constellation_Network.CoinTable C ON C.sk = U.sk
                WHERE D.state = '재적'
                """ + searchClause + """
                ORDER BY D.username ASC, U.discordID ASC
                LIMIT :size OFFSET :offset
                """);
        setSearchParameter(query, discordId);
        query.setParameter("size", size);
        query.setParameter("offset", offset);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return rows;
    }

    public long countActivePointMembers(String discordId) {
        String searchClause = discordId == null ? "" : " AND U.discordID = :discordId";
        Query query = entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM Constellation_Network.Users U
                JOIN Constellation_Network.DiscordUsers D ON D.discordID = U.discordID
                WHERE D.state = '재적'
                """ + searchClause);
        setSearchParameter(query, discordId);
        return ((Number) query.getSingleResult()).longValue();
    }

    public Object[] findActivePointMemberByDiscordId(String discordId) {
        List<?> rows = entityManager.createNativeQuery("""
                SELECT U.discordID, D.username, U.sk, COALESCE(C.coin, 0) AS coin
                FROM Constellation_Network.Users U
                JOIN Constellation_Network.DiscordUsers D ON D.discordID = U.discordID
                LEFT JOIN Constellation_Network.CoinTable C ON C.sk = U.sk
                WHERE D.state = '재적'
                  AND U.discordID = :discordId
                """)
                .setParameter("discordId", discordId)
                .setMaxResults(1)
                .getResultList();
        return rows.isEmpty() ? null : (Object[]) rows.get(0);
    }

    public void depositCoin(String sk, int amount) {
        entityManager.createNativeQuery("""
                INSERT INTO Constellation_Network.CoinTable (sk, coin)
                VALUES (:sk, :amount)
                ON DUPLICATE KEY UPDATE coin = coin + :amount
                """)
                .setParameter("sk", sk)
                .setParameter("amount", amount)
                .executeUpdate();
    }

    public int withdrawCoin(String sk, int amount) {
        return entityManager.createNativeQuery("""
                UPDATE Constellation_Network.CoinTable
                SET coin = coin - :amount
                WHERE sk = :sk
                  AND coin >= :amount
                """)
                .setParameter("sk", sk)
                .setParameter("amount", amount)
                .executeUpdate();
    }

    public void insertPointLog(
            String sk,
            int amount,
            LocalDateTime at,
            String description
    ) {
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

    public long findCoinBySk(String sk) {
        List<?> rows = entityManager.createNativeQuery("""
                SELECT coin
                FROM Constellation_Network.CoinTable
                WHERE sk = :sk
                """)
                .setParameter("sk", sk)
                .getResultList();
        return rows.isEmpty() ? 0L : ((Number) rows.get(0)).longValue();
    }

    private void setSearchParameter(Query query, String discordId) {
        if (discordId != null) {
            query.setParameter("discordId", discordId);
        }
    }
}
