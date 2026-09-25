package com.help.erpweb.domain.membership.repository;

import com.help.global.chat.ChatUser;
import com.help.global.guild.GuildContext;
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

    /**
     * ADR-0001: 기존 search_sk(cardType, membershipId)는 봇 저장소가 그대로 쓰므로
     * 그대로 두고, botId까지 포함해 sk를 조회하는 search_sk_by_bot(botId, cardType,
     * membershipId)를 별도로 새로 만들어 쓴다(봇 코드와의 시그니처 충돌 회피).
     * (동일 externalId라도 bot마다 다른 sk가 나올 수 있음).
     * botId는 요청을 처리 중인 스레드의 GuildContext에서 가져온다.
     * <p>
     * ChatUser 도메인 모델을 받아 cardType/membershipId를 platform에서
     * 그대로 뽑아 쓰므로, discord뿐 아니라 ChatUser의 다른 구현체(예: 카카오톡)가
     * 생겨도 이 메서드는 바뀌지 않는다.
     */
    public String findSkByChatUser(ChatUser chatUser) {
        return (String) entityManager
                .createNativeQuery(
                        "SELECT Constellation_Network.search_sk_by_bot(:botId, :cardType, :membershipId)"
                )
                .setParameter("botId", GuildContext.requireBotId())
                .setParameter("cardType", chatUser.getPlatform().name())
                .setParameter("membershipId", chatUser.getExternalId())
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
