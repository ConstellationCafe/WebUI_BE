package com.help.authserver.domain.user.entity;

import com.fasterxml.jackson.annotation.JsonAlias;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SessionInfo {

    // ADR-0008: 이전 이름(username)으로 Redis에 저장된 세션도 읽을 수 있게 한다.
    @JsonAlias("username")
    private String discordId;

    private boolean valid;

    private String discordAccessToken;

    private long issuedAt;
}
