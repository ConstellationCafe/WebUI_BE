package com.help.global.discord.config;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ERPSubscriberRepository extends JpaRepository<ErpSubscriber, String> {
    List<ErpSubscriber> findByGuildIdIn(List<String> guildIds);
    List<ErpSubscriber> findByDiscordId(String discordId);
    Optional<ErpSubscriber> findByGuildId(String guildId);
}
