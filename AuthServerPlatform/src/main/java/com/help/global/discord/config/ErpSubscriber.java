package com.help.global.discord.config;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * authserver/erpweb 서비스 분리 준비로 com.help.global 아래로 옮겨왔다.
 * 로그인(guildId -> botId 조회)과 erpweb academy 도메인(등록된 guild 확인)
 * 양쪽에서 읽는다.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "erp_subscriber")
@Entity
public class ErpSubscriber {
    @Id
    @Column(name = "bot_id")
    private String botId;
    @Column(name = "guild_id")
    private String guildId;
    @Column(name = "discord_id")
    private String discordId;
    @Column(name = "subscribe_at")
    private LocalDateTime subscribeAt;
}
