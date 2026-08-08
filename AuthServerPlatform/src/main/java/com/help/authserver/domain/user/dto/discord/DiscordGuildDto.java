package com.help.authserver.domain.user.dto.discord;

public record DiscordGuildDto(
        String id,
        String name,
        String icon
) {}