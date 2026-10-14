package com.help.global.discord.identity;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DiscordUserRepository extends JpaRepository<DiscordUser, Long> {

    // ADR-0001: 로그인 완료(채팅방 선택) 이전, discordId 신원 확인에만 사용한다.
    // botId 스코프가 없으므로 결과의 roles(방별 admin 여부)는 신뢰할 수 없다 —
    // 이 결과의 getRoleName()을 인가(authorization) 판단에 사용하지 말 것.
    // 같은 discordId로 여러 행(여러 방)이 있을 수 있으므로 임의의 한 행을 고른다.
    @EntityGraph(attributePaths = "roles")
    List<DiscordUser> findByDiscordIdOrderByIdAsc(String discordId);

    default Optional<DiscordUser> findIdentityByDiscordId(final String discordId) {
        final List<DiscordUser> rows = findByDiscordIdOrderByIdAsc(discordId);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    // botId(=채팅방) 스코프 조회. 로그인 완료(채팅방 선택) 이후 /api/**에서만 사용한다.
    // 이 결과가 비어있다는 것은 "등록된 방이지만 이 사용자가 그 방의 멤버가 아니다"를
    // 뜻하며, /auth/guild/select의 멤버십 검증과 BackEndJwtAuthFilter의 인증 모두
    // 여기에 의존한다.
    @EntityGraph(attributePaths = "roles")
    @Query("SELECT u " +
           "FROM DiscordUser u " +
           "WHERE u.botId = :botId " +
           "AND u.discordId = :discordId " +
           "AND u.state = '재적'")
    Optional<DiscordUser> findByBotIdAndDiscordId(
            @Param("botId") String botId,
            @Param("discordId") String discordId
    );

    @EntityGraph(attributePaths = "roles")
    @Query("SELECT u " +
            "FROM DiscordUser u " +
            "WHERE u.botId = :botId " +
            "AND u.discordId = :discordId ")
    Optional<DiscordUser> findAllByBotIdAndDiscordId(
            @Param("botId") String botId,
            @Param("discordId") String discordId
    );

    @Query("""
        SELECT u
        FROM DiscordUser u
        WHERE u.botId = :botId
          AND u.discordId IN :discordIds
          AND u.state = '재적'
    """)
    List<DiscordUser> findActiveByBotIdAndDiscordIdIn(
            @Param("botId") String botId,
            @Param("discordIds") List<String> discordIds
    );

    @Query("""
        SELECT u
        FROM DiscordUser u
        WHERE u.botId = :botId
          AND u.discordId IN :discordIds
    """)
    List<DiscordUser> findAllByBotIdAndDiscordIdIn(
            @Param("botId") String botId,
            @Param("discordIds") List<String> discordIds
    );
}
