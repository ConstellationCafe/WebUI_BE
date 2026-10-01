package com.help.global.discord.identity;

import com.help.global.data.Authority;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * authserver/erpweb 서비스 분리 준비: 이 엔티티는 로그인(authserver)과
 * 여러 erpweb 도메인 서비스(academy 등) 양쪽에서 읽으므로 com.help.global
 * 아래로 옮겨왔다. 더 이상 authserver의 로그인 전용 User 인터페이스를
 * 구현하지 않는다 — 그 인터페이스는 EmailUser(순수 authserver 개념)를 위한
 * 것이었고, DiscordUser가 그걸 구현해야만 했던 유일한 이유는
 * CustomUser.from(User)였다. CustomUser는 이제 원시 값(username/password/
 * roleName/userId)만 받는 CustomUser.of(...)를 쓰므로 이 인터페이스가
 * 필요 없다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "DiscordUsers")
@Entity
public class DiscordUser {
    // ADR-0001: discordID 단독으로는 더 이상 전역 유일하지 않다 —
    // (botId, discordID) 조합으로만 유일하며, 같은 사람이 여러 방에 각각
    // 별도의 행(그리고 별도의 역할)을 가질 수 있다. JPA @Id는 단일 컬럼이
    // 필요해 surrogate key(id)를 도입했지만, 이 id는 DiscordUsers 자체의
    // PK로만 쓰인다.
    //
    // RoleTable은 이 surrogate id를 참조하지 않는다 — 디스코드 봇이 여전히
    // discordID 컬럼으로 role을 기록하므로(별도 저장소, 여기서 바꿀 수
    // 없음), discordID 컬럼을 그대로 유지한 채 RoleTable에도 bot_id를
    // 추가해서 DiscordUsers와 동일한 (bot_id, discordID) 조합으로 조인한다.
    // 봇이 role을 쓸 때 bot_id를 같이 기록해야 방 단위 역할 구분이 실제로
    // 성립한다(봇 저장소 쪽 후속 작업 필요).
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // guildId와 1:1이며, 내부 전파/DB 스코프 단위는 botId로 통일한다.
    @Column(name = "bot_id", nullable = false)
    private String botId;

    @Column(nullable = false)
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
            joinColumns = {
                    @JoinColumn(name = "bot_id", referencedColumnName = "bot_id"),
                    @JoinColumn(name = "discordID", referencedColumnName = "discordID")
            }
    )
    @Column(name = "role_name")
    private List<String> roles = new ArrayList<>();

    private DiscordUser(
        String botId,
        String discordID,
        List<String> roles
    ) {
        this.botId = botId;
        this.discordID = discordID;
        this.roles = roles;
    }

    public static DiscordUser of(
        String botId,
        String discordID,
        List<String> roles
    ) {
        // 테스트를 위한 DiscordUser 객체 생성
        return new DiscordUser(botId, discordID, roles);
    }

    public String getUsername() {
        return discordID;
    }

    public String getNickname() {
        return username;
    }

    /**
     * Spring Security authority 문자열("ROLE_ADMIN"/"ROLE_USER").
     * 예전에는 authserver의 UserRole enum을 반환했지만(User 인터페이스
     * 계약), 그 인터페이스를 더 이상 구현하지 않으므로 global의 Authority
     * 상수를 직접 쓴다.
     */
    public String getRoleName() {
        boolean isAdmin = roles.stream()
                .anyMatch("서버장"::equals);
        return isAdmin ? Authority.ADMIN : Authority.USER;
    }

    public String getPassword() {
        return "OAUTH_USER";
    }
}
