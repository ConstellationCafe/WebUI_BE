package com.help.authserver.domain.user.dto.user;

public record DiscordUserDto (
    String discordId,
    String username,
    String globalName,
    String avatar
) {}