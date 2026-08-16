package com.help.erpweb.domain.membership.repository;

import com.help.global.data.MembershipID;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

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
}