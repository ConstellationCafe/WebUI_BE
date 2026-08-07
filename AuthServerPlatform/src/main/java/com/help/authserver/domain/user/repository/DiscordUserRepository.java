package com.help.authserver.domain.user.repository;

import com.help.authserver.domain.user.entity.DiscordUser;
import com.help.authserver.domain.user.entity.ErpSubscriber;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DiscordUserRepository extends JpaRepository<DiscordUser, String> {
    @EntityGraph(attributePaths="roles")
    @Query("SELECT u " +
           "FROM DiscordUser u " +
           "WHERE u.discordID = :discordID " +
           "AND u.state = '재적'")
    Optional<DiscordUser> findByDiscordID(String discordID);
}
