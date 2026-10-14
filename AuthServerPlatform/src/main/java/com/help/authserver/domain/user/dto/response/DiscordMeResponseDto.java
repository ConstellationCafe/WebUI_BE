package com.help.authserver.domain.user.dto.response;
import com.fasterxml.jackson.annotation.JsonProperty;

public record DiscordMeResponseDto(
        String id,
        // Discord API의 username = 계정 username (서버 표시 이름 아님)
        @JsonProperty("username")
        String discordUsername,
        @JsonProperty("global_name")
        String globalName,
        String avatar
) {}
