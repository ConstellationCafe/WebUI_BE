package com.help.authserver.domain.user.repository.config;

import com.help.authserver.domain.user.entity.config.ErpSubscriber;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ERPSubscriberRepository extends JpaRepository<ErpSubscriber, String> {
    List<ErpSubscriber> findByGuildIdIn(List<String> guildIds);
    List<ErpSubscriber> findByDiscordId(String discordId);  // 임시로 만든 기능
    Optional<ErpSubscriber> findByGuildId(String guildId);
}