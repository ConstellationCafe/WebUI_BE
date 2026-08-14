package com.help.authserver.domain.user.repository.config;

import com.help.authserver.domain.user.entity.config.ErpSubscriber;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ERPSubscriberRepository extends JpaRepository<ErpSubscriber, String> {
    List<ErpSubscriber> findByGuildIdIn(List<String> guildIds);
    List<ErpSubscriber> findByDiscordId(String discordId);
}