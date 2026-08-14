package com.help.authserver.domain.user.entity.constellation;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "DiscordUsers")
@Entity
public class DiscordUser implements User {
    @Id
    private String discordID;

    @Column
    private String username;

    @Column(nullable = false)
    private String state;

    @Column
    private LocalDateTime join_at;

    @Column
    private LocalDateTime left_at;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "RoleTable",
            joinColumns = @JoinColumn(name = "discordID", referencedColumnName = "discordID")
    )
    @Column(name = "role_name")
    private List<String> roles = new ArrayList<>();

    private DiscordUser(
        String discordID,
        List<String> roles
    ) {
        this.discordID = discordID;
        this.roles = roles;
    }

    public static DiscordUser of(
        String discordID,
        List<String> roles
    ) {
        // 테스트를 위한 DiscordUser 객체 생성
        return new DiscordUser(discordID, roles);
    }

    @Override
    public UUID getUserId() {
        return null;
    }

    @Override
    public String getUsername() { return discordID; }

    @Override
    public UserRole getRole() {
        boolean isAdmin = roles.stream()
                .anyMatch("서버장"::equals);
        if (isAdmin)
            return UserRole.ADMIN;
        else
            return UserRole.USER;
    }

    @Override
    public String getPassword() {
        return "OAUTH_USER";
    }

    @Override
    public UserProfile getUserProfile() {
        return null;
    }
}
