package com.help.authserver.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * ADR-0001: 로그인은 discordId 인증만으로는 완료되지 않고,
 * 이 요청(/auth/guild/select)으로 채팅방(guildId)을 선택해야 완료된다.
 */
public record GuildSelectRequestDto(
        @NotBlank String guildId
) {
}
