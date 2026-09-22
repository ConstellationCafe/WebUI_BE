package com.help.authserver.domain.user.repository.config;

import com.help.authserver.domain.user.entity.config.ErpSubscriber;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ERPSubscriberRepository extends JpaRepository<ErpSubscriber, String> {
    List<ErpSubscriber> findByGuildIdIn(List<String> guildIds);
    List<ErpSubscriber> findByDiscordId(String discordId);  // ììë¡ ë§ë  ê¸°ë¥
    Optional<ErpSubscriber> findByGuildId(String guildId);
}
