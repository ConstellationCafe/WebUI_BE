package com.help.authserver.domain.user.service;

import com.help.global.discord.identity.DiscordUser;
import com.help.global.discord.identity.DiscordUserRepository;
import com.help.global.jwt.CustomUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class DiscordIdentityResolver implements IdentityResolver {

    private final DiscordUserRepository userRepository;

    @Override
    public Optional<CustomUser> resolveIdentity(final String username, final String botId) {
        // ADR-0001: botId가 있으면(=채팅방을 이미 선택했으면) 그 방 기준으로
        // 스코프해서 조회한다 — admin 여부(roles)가 방 단위로 갈리므로,
        // 스코프 없이 조회하면 임의의(다른) 방의 roles를 실어버릴 수 있다.
        // botId가 없으면 아직 채팅방 선택 이전(신원 확인 전용) 조회다.
        final Optional<DiscordUser> user =
                botId != null
                        ? userRepository.findByBotIdAndDiscordID(botId, username)
                        : userRepository.findIdentityByDiscordID(username);

        return user.map(DiscordUserMapper::toCustomUser);
    }
}
