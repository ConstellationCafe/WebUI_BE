package com.help.authserver.domain.user.dto.response;

public record DiscordGuildResponseDto(
        String id,
        String name,
        String icon
) {}