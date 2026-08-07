package com.help.authserver.domain.user.dto.user;

import com.help.authserver.domain.user.dto.discord.DiscordGuildDto;

import java.util.List;

public record DiscordUserDto (
    String discordId,
    String username,
    String globalName,
    String avatar,
    List<DiscordGuildDto> guilds
) {}