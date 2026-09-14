package com.help.erpweb.domain.membership.repository;

import com.help.global.data.MembershipID;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

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

    public Map<String, String> findDiscordIdsBySk(
            List<String> sks
    ) {
        if (sks == null || sks.isEmpty()) {
            return Map.of();
        }

        List<Object[]> results = entityManager
                .createNativeQuery(
                    """
                        SELECT sk, discordID
                        FROM Constellation_Network.Users
                        WHERE sk IN (:sks)
                    """
                )
                .setParameter("sks", sks)
                .getResultList();

        Map<String, String> result = new HashMap<>();
        for (Object[] row : results) {
            if (row[0] == null || row[1] == null) {
                continue;
            }
            result.put(
                    row[0].toString(),
                    row[1].toString()
            );
        }
        return result;
    }
}