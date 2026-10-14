package com.help.authserver.domain.user.dto.discord;

public record DiscordMeDto(
        String id,
        String discordUsername,
        String globalName,
        String avatar
) {}
