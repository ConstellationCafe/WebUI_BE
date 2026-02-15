package com.help.authserver.domain.user.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SessionInfo {

    private String username;

    private boolean valid;

    private String discordAccessToken;

    private long issuedAt;
}