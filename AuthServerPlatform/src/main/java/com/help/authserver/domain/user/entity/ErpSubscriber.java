package com.help.authserver.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
    @Column(name = "subscribe_at")
    private LocalDateTime subscribeAt;
}
