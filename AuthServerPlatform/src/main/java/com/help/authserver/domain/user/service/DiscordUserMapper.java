package com.help.authserver.domain.user.service;

import com.help.global.discord.constellation.DiscordUser;
import com.help.global.jwt.CustomUser;

/**
 * authserver/erpweb 서비스 분리 준비 작업(이전 세션)에서 CustomUser가
 * authserver의 User 인터페이스를 몰라도 되도록 CustomUser.of(원시값)만
 * 받게 바뀌었다. DiscordUser -> CustomUser 변환은 그 원시값을 뽑아 넘기는
 * 자리이고, DiscordLoginService와 DiscordIdentityResolver 양쪽에서 동일하게
 * 필요해 여기로 뽑아 공유한다.
 */
final class DiscordUserMapper {

    private DiscordUserMapper() {
    }

    static CustomUser toCustomUser(final DiscordUser user) {
        return CustomUser.of(
                user.getUsername(),
                user.getPassword(),
                user.getRoleName(),
                null
        );
    }
}
