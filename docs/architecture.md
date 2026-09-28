# WebUI_BE Architecture

> 상태: Active  
> 마지막 검토일: 2026-09-28  
> 상위 문서: [README](../README.md) · 관련: [API 개요](api.md), [ADR 목록](adr/README.md), [운영](operations.md)

## 1. 시스템 구성

```text
┌──────────────────┐  HTTP / SSE   ┌───────────────────┐      ┌──────────────────────────┐
│  WebUI_FE        │ ────────────► │  WebUI_BE         │ ───► │ MySQL                     │
│  Flutter Web     │   쿠키 인증    │  Spring Boot      │      │  - Constellation_Network │
│  nginx :1104     │ ◄──────────── │  :4003            │      │  - config DB             │
└──────────────────┘               └───────────────────┘      └──────────────────────────┘
                                     │        │
                     X-Api-Key       │        ├──► Redis :6379
┌──────────────────┐  ─────────────► │        │     - 인증 세션 session:user:{username}
│ 외부 시스템        │ /api/integrations        │     - 알림 Pub/Sub webui:notifications
│ (Discord 봇 등)   │                          └──► Discord OAuth / API
└──────────────────┘
```

- MySQL은 두 datasource를 씁니다. 주 DB(`spring.datasource.constellation`)는 사용자·회원·역할, Academy, 추천 저장소, 포인트, 벌점, 알림을, config DB(`spring.datasource.config_db`)는 ERP subscriber(guild ↔ botId)와 module 설정을 담습니다.
- 같은 DB를 빗자루 봇([ModularDiscordBot](https://github.com/ConstellationCafe/ModularDiscordBot))도 읽고 씁니다. schema 변경은 [migration 원칙](migrations/README.md)을 따릅니다.
- 일부 FE 기능(친선전, 일부 Academy·멤버십 조회)은 WebUI_BE를 거치지 않고 빗자루 봇 router를 직접 호출합니다.

## 2. 패키지 구조

`AuthServerPlatform/src/main/java/com/help`

| 패키지 | 책임 |
|---|---|
| `authserver.api` | Discord OAuth/API 호출(`DiscordAPI`) |
| `authserver.domain.user` | `/auth/**` controller, 로그인·세션(`DiscordLoginService`, `AuthSessionService`), Redis 세션 repository |
| `authserver.security` | `/auth/**` 전용 JWT 필터 |
| `erpweb.domain.academy` | Academy API와 권한 판정(`AcademyAuthorization`, bean 이름 `academyAuth`) |
| `erpweb.domain.modules.chatbot.{content,learning,menu,music}` | 추천 저장소 API, stored procedure 호출 |
| `erpweb.domain.modules.erp.point` | 본인 포인트 내역(`PointController`), 관리자 포인트(`AdminPointController`), 공통 `PointService` |
| `erpweb.domain.modules.erp.penalty` | 벌점 API (ADR-0003) |
| `erpweb.domain.notification` | 알림 발행·조회, `realtime/`의 Redis 중계와 SSE registry (ADR-0004) |
| `erpweb.domain.config`, `erpweb.domain.metadata` | config DB entity, 동적 metadata(검색·정렬 허용 컬럼) |
| `global.config` | datasource 2개, Redis, Discord `RestTemplate`(timeout), `Clock`, security |
| `global.jwt` | `JwtUtil`, `/api/**` JWT 필터(`BackEndJwtAuthFilter`) |
| `global.guild`, `global.chat`, `global.discord` | 요청 단위 채팅방 context(`GuildContext`), 채팅방·회원 식별 |
| `global.integration` | 외부 연동 `X-Api-Key` 인증 chain |
| `global.common` | 공통 `ApiResponse`, `ErrorCode`, `GlobalExceptionHandler` |

## 3. 요청 흐름과 계층 책임

```text
Security chain → Controller → Service → Repository → MySQL
                   │            │
                   │            └── 외부 경계: Discord API, Redis
                   └── 입력 검증(Bean Validation), 인증 주체 확인, DTO 변환
```

- **Controller**: 인증 주체 확인, request DTO 검증, 응답 DTO 변환, `@PreAuthorize` 인가. 업무 규칙을 두지 않습니다.
- **Service**: 업무 규칙과 transaction 경계. 인가 수준이 다르면 controller는 나누고 로직은 service 하나로 합칩니다(예: `PointController`/`AdminPointController` → `PointService`).
- **Repository**: JPA, native query, stored procedure. 채팅방 범위는 `GuildContext.requireBotId()`로 얻은 `botId`로 제한합니다.
- entity를 API로 그대로 노출하지 않고 request/response DTO를 거칩니다.

## 4. 인증과 채팅방 스코프 ([ADR-0001](adr/0001-guild-scope.md))

1. `GET /auth/discord_login` — Discord OAuth code 교환, Redis 세션 생성, `botId` 없는 토큰 발급
2. `GET /auth/guilds` — 가입한 채팅방 목록
3. `POST /auth/guild/select` — guildId → botId 변환, 방 회원 확인, 방 기준 역할로 `botId` 토큰 재발급
4. `/api/**` — `BackEndJwtAuthFilter`가 `botId`를 `GuildContext`에 넣고 요청 끝에 비움

## 5. Security chain

| 순서 | 대상 | 인증 방식 | 실패 |
|---|---|---|---|
| `@Order(0)` | `/api/integrations/**` | `X-Api-Key`의 SHA-256을 설정 해시와 constant-time 비교, client별 허용 botId | 401 / 403 |
| — | `/auth/**` | 일부 공개, 나머지 AccessToken + Redis 세션 | 본문 없는 401 |
| — | `/api/**` | AccessToken, `authenticated()`. `/api/admin/**`은 `ROLE_ADMIN` | 401 JSON, 비관리자 404 |

- 관리자 API는 URL 규칙과 controller `@PreAuthorize`로 이중 확인합니다([ADR-0002](adr/0002-admin-api-prefix.md)).
- SSE의 비동기 dispatch(`DispatcherType.ASYNC`)는 다시 인가하지 않습니다(최초 요청에서 이미 인가됨).

## 6. 알림 실시간 전달 ([ADR-0004](adr/0004-notification.md))

```text
발행(관리자·내부·외부) → NotificationService: DB 저장(원본)
      → 커밋 후 Redis Pub/Sub `webui:notifications`
      → 각 인스턴스가 자기 SSE 연결에 전달 (at-most-once)
놓친 알림 → REST(`/api/me/notifications`, `/unread-count`)로 복구
```
