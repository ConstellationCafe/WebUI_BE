package com.help.authserver.domain.user.dto.response;
import com.fasterxml.jackson.annotation.JsonProperty;

public record DiscordMeResponseDto(
        String id,
        String username,
        @JsonProperty("global_name")
        String globalName,
        String avatar
) {}