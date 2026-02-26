package com.help.authserver.domain.user.dto.user;

import java.util.List;

public record AuthMeDto (
    String discordId,
    String username,
    String globalName,
    String avatar,
    List<String> roles
) {}