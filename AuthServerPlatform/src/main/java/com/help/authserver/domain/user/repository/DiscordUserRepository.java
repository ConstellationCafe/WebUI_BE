package com.help.authserver.domain.user.repository;

import com.help.authserver.domain.user.entity.DiscordUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface DiscordUserRepository extends JpaRepository<DiscordUser, Long> {
    @Transactional(readOnly = true)
    Optional<DiscordUser> findByDiscordID(String discordID);
}
