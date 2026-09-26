package com.help.authserver.domain.user.service;

import com.help.global.jwt.CustomUser;

import java.util.Optional;

/**
 * AuthSessionService(JWT/Redis, 프로바이더 무관)가 토큰 재발급·채팅방 선택
 * 시점에 "이 사용자의 최신 신원/역할"을 다시 조회해야 할 때 쓰는 창구.
 *
 * botId가 null이면 아직 채팅방을 선택하지 않은 상태의 신원 확인(ADR-0001
 * 1단계), botId가 있으면 그 방 기준으로 스코프된 조회다. 실제 조회는
 * 프로바이더별 저장소(Discord는 {@link DiscordUserRepository})가 하므로,
 * AuthSessionService는 이 인터페이스만 알면 되고 어떤 프로바이더인지는
 * 몰라도 된다.
 */
public interface IdentityResolver {

    Optional<CustomUser> resolveIdentity(String username, String botId);
}
