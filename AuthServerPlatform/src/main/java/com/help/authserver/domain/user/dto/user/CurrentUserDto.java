package com.help.authserver.domain.user.dto.user;

import java.util.List;

public record CurrentUserDto(
    String discordId,
    String discordUsername,
    String globalName,
    String avatar,
    List<String> roles
) {}
