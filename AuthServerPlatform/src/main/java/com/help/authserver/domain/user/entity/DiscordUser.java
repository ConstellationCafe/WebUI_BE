package com.help.authserver.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "DiscordUsers")
@Entity
public class DiscordUser {
    @Id
    private String discordID;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String state;

    @Column
    private LocalDateTime join_at;

    @Column
    private LocalDateTime left_at;
}
