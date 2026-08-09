package com.help.authserver.domain.user.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DiscordGuildResponseDto(
        String id,
        String name,
        String icon,
        @JsonProperty("approximate_member_count")
        Integer memberCount
) {}