# ADR-0001: 채팅방(길드) 스코프를 `botId`로 통일하고 로그인에 채팅방 선택 단계를 둔다

- 상태: Accepted (사후 기록)
- 결정 시기: 2026-09-25 ~ 2026-09-26 (`refactor/guild-scope-context`, WebUI_BE PR #46으로 `develope` 병합)
- 기록일: 2026-09-28
- 적용 범위: WebUI_BE, WebUI_FE, `Constellation_Network` DB
- 담당자: 미정 (프로젝트 문서 담당자 지정 필요)
- 관련: [0001 migration](../migrations/0001_guild_scope_bot_id.sql), [ADR-0002](0002-admin-api-prefix.md), [ADR-0003](0003-penalty-log.md), [ADR-0004](0004-notification.md)

> 이 ADR은 결정이 코드·migration·다른 ADR에서 "ADR-0001"로 참조되고 있으나 원문 파일이 없어, 병합된 구현(`GuildContext`, `JwtUtil`, `AuthSessionService`)과 0001 migration 주석, Notion 회고(2026-09-26)를 근거로 사후 작성했다. 결정 당시 검토한 대안 중 기록으로 확인되지 않은 내용은 적지 않았다.

## Context

- 빗자루는 여러 Discord 채팅방(길드)에서 운영될 예정이고, guild와 봇 인스턴스(`botId`)는 1:1이다.
- 기존 구현은 사용자를 `discordId` 하나로 식별했다. 같은 Discord 계정이 여러 채팅방에 속하면 Academy 멤버십, 포인트, 역할(ADMIN 여부) 같은 데이터가 채팅방 사이에 섞여 노출될 수 있었다.
- 관리자 여부는 전역이 아니라 채팅방마다 다르다.
- 봇(별도 저장소 [ModularDiscordBot](https://github.com/ConstellationCafe/ModularDiscordBot))이 같은 DB의 `discordID` 컬럼과 기존 저장 함수 `search_sk`를 계속 쓰고 있어, 기존 컬럼·함수를 바꾸면 봇이 깨진다.

## Decision

1. **스코프 단위는 `botId`다.** 애플리케이션 내부 전파와 DB 스코프를 모두 `botId`로 통일한다. 채팅방 안의 회원은 `(botId, discordId)`로, 방 단위 사용자 식별자 `sk`는 `(botId, discordId)`로 정한다.
2. **로그인은 두 단계다.** Discord OAuth 인증만으로는 로그인이 끝나지 않는다. `POST /auth/guild/select`로 채팅방을 선택하면
   - `ERPSubscriber`에서 guildId → botId를 찾고(없으면 403 `GUILD_NOT_REGISTERED`),
   - 사용자가 그 방의 회원인지 확인한 뒤(아니면 403 `GUILD_MEMBER_NOT_FOUND`),
   - 그 방 기준으로 다시 조회한 역할로 `botId` claim이 담긴 AccessToken·RefreshToken을 발급한다.
3. **`botId` claim이 없는 토큰은 "선택 대기" 상태다.** `/api/**`는 `BackEndJwtAuthFilter`가 토큰의 `botId`를 요청 단위 `GuildContext`(ThreadLocal)에 넣고, 서비스·repository는 `GuildContext.requireBotId()`로 현재 방을 얻는다. RefreshToken에도 `botId`를 실어 30초마다 재발급해도 선택 상태가 유지되게 한다.
4. **`refresh`와 `selectRoom`은 같은 방식으로 역할을 다시 조회한다.** 방 선택 직후 이전 권한으로 토큰이 발급되는 시간창을 없앤다(2026-09-26 수정).
5. **DB 변경은 옆에 추가하는 방식(expand-only)으로만 한다.** `DiscordUsers`, `RoleTable`, `Users`에 `bot_id` 컬럼을 추가하고, 기존 `search_sk`는 그대로 둔 채 `search_sk_by_bot(p_bot_id, card_type, membershipID)`를 새로 만든다. WebUI_BE는 새 함수만 호출한다.
6. `DiscordUsers`의 PK를 `discordID`에서 `id`로 바꾸는 작업은 봇이 INSERT에 `bot_id`를 채우도록 바뀐 뒤로 미룬다.

## Consequences

- WebUI_FE는 로그인 후 반드시 `/select`에서 채팅방을 고르고, `/home?guild_id=`는 선택이 끝난 토큰과 사용자 길드 목록에 있는 `guild_id`일 때만 진입한다.
- 이후 기능(벌점 ADR-0003, 알림 ADR-0004)은 처음부터 `botId` 단위로 설계한다.
- Academy 권한은 스키마 변경 없이 `sk` 기준으로 판정하도록 바뀌어, 다른 방의 멤버십이 섞이지 않는다. 다만 `academyId` 없이 전체 조회할 때 데이터 범위를 방·Academy로 좁히는 작업은 남아 있다.
- 운영 DB에는 0001 migration의 남은 작업과 `bot_id` backfill을 수동으로 적용해야 한다. backfill이 끝나기 전 빈 `bot_id` 행은 어느 방에도 속하지 않는다.
- 봇 저장소가 `bot_id`를 채우기 전까지는 두 번째 채팅방을 등록하면 봇 쪽 데이터가 기존 방으로 기록될 수 있다.

## 후속 작업

- [ ] 봇 저장소가 `DiscordUsers`·`RoleTable`·`Users` INSERT에 `bot_id`를 채우도록 변경
- [ ] 그 이후 `DiscordUsers` PK를 `id`로 교체, `bot_id` DEFAULT 제거와 인덱스 추가(0001 migration 4번)
- [ ] 사용하지 않는 `RoleTable.discord_user_id` 정리 여부 결정
- [ ] `academyId` 없는 Academy 전체 조회의 데이터 스코핑
- [ ] 문서 담당자 지정
