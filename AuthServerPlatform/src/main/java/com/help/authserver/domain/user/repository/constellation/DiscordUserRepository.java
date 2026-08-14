package com.help.authserver.domain.user.repository.constellation;

import com.help.authserver.domain.user.entity.constellation.DiscordUser;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface DiscordUserRepository extends JpaRepository<DiscordUser, String> {
    @EntityGraph(attributePaths="roles")
    @Query("SELECT u " +
           "FROM DiscordUser u " +
           "WHERE u.discordID = :discordID " +
           "AND u.state = '재적'")
    Optional<DiscordUser> findByDiscordID(String discordID);
}
