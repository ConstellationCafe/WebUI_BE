package com.help.authserver.domain.user.dto.discord;

public record DiscordMeDto(
        String id,
        String username,
        String globalName,
        String avatar
) {}