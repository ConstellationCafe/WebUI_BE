# WebUI_BE API 개요

> 상태: Active  
> 적용 범위: WebUI_BE HTTP API (`develope` 기준)  
> 마지막 검토일: 2026-10-04
> 상위 문서: [README](../README.md)

이 문서는 저장소 안에서 API를 빠르게 찾기 위한 요약입니다. **request/response·권한·오류 계약의 기준은 Notion 명세**이며, API를 바꾸면 같은 작업에서 Notion 해당 페이지를 먼저 갱신하고 이 문서도 맞춥니다.

## 1. 명세 위치

Notion `섀버 별자리 Cafe 개발 본부 / 명세서 / API 명세서`

| Notion 페이지 | 범위 |
|---|---|
| WebUI_BE API 공통 규격 | 인증·쿠키, CORS, 공통 응답·오류, 관리자 경로 규칙, 전체 엔드포인트 목록 |
| Auth API 명세 | `/auth/**` |
| Academy API 명세 | `/api/academy/**` |
| ChatBot API 명세 | `/api/repository/{content,learning,menu,music}/**` |
| Membership API 명세 | `/api/repository/membership/point_log`, `/api/admin/points/**` |
| Shadowverse API 명세 | 빗자루 봇 router 경유 기능(WebUI_BE endpoint 없음) |
| Penalty API 명세 | `/api/penalties/**`(호환: `/api/admin/penalties/**`), `/api/me/penalties` |
| Notification API 명세 | `/api/me/notifications/**`, `/api/admin/notifications`, `/api/integrations/notifications` |
| Competition API 명세 | `/api/competitions/**` |
| ModuleConfig API 명세 | `/api/bots/current/module-configs` |

## 2. 공통 규칙

### 인증과 보안 chain

| 경로 | 인증 | 실패 응답 |
|---|---|---|
| `/auth/discord_login`, `/auth/check`, `/auth/refresh` | 공개 (`refresh`는 RefreshToken 쿠키 필요) | — |
| 그 밖의 `/auth/**` | AccessToken + 유효한 Redis 세션 + 활성 Discord 사용자 | 본문 없는 401 |
| `/api/**` | AccessToken, 채팅방 선택을 마친(`botId` claim이 있는) 토큰 ([ADR-0001](adr/0001-guild-scope.md)) | 401 + `UNAUTHORIZED` JSON |
| `/api/admin/**` | 위 조건 + `ROLE_ADMIN` ([ADR-0002](adr/0002-admin-api-prefix.md)) | 인증된 비관리자는 404 `NOT_FOUND` |
| `/api/integrations/**` | `X-Api-Key` 서비스 키만 인정(사용자 쿠키 무시), 별도 chain `@Order(0)` ([ADR-0004](adr/0004-notification.md)) | 401 / 허용 밖 botId는 403 |

- 토큰: `AccessToken`(30초), `RefreshToken`(1일) HttpOnly 쿠키. 쿠키 `Secure`는 profile이 `prod`일 때만 켜집니다.
- CORS: `/auth/**`, `/api/**` 모두 `FRONT_REDIRECT_URI` 하나만 허용, credential 허용. `/api/integrations/**`는 CORS를 열지 않습니다.
- CSRF는 비활성화되어 있고 서버 HTTP session은 `STATELESS`입니다(세션 유효성은 Redis로 확인).

### 응답과 오류

```json
{ "success": true, "response": {}, "error": null }
{ "success": false, "response": null, "error": { "message": "오류 메시지", "status": 400 } }
```

| 상황 | HTTP |
|---|---|
| validation, 형식 오류, JSON 파싱 오류, 필수 파라미터 누락 | 400 |
| 미인증, 토큰 만료·무효 | 401 |
| 등록되지 않은 채팅방(`GUILD_NOT_REGISTERED`), 채팅방 비회원(`GUILD_MEMBER_NOT_FOUND`) | 403 |
| 인증된 비관리자의 관리자 API 호출, 없는 자원·handler | 404 |
| 같은 요청 ID·다른 내용, 잔액 부족, 내역 충돌 | 409 |
| Discord 등 외부 서비스 장애 | 503 |
| 그 밖의 예외 | 500 |

예외: `GET /api/academy/lesson-records`는 공통 래퍼 없이 배열을 직접 반환합니다.

### 경로 규칙 ([ADR-0002](adr/0002-admin-api-prefix.md))

- 관리자 전용 API는 `/api/admin/**`, 본인 데이터는 `/api/me/**` 아래에 둡니다. 서버장 외 역할도 쓰는 관리 기능(대회, 벌점)은 `/api/admin/**` 밖에 두고 기능별 `@PreAuthorize`로 확인합니다([ADR-0006](adr/0006-penalty-manager-role.md)).
- 새 경로는 kebab-case와 복수형 명사, 동작은 HTTP method로 표현합니다. 내부 계층·패키지 이름(`repository`, `erp`, `modules`)은 경로에 넣지 않습니다.
- version 접두사(`/v1`)는 두지 않습니다. 외부 client에 공개할 때 다시 결정합니다.
- 기존 `/api/repository/...`, `/api/academy/...`는 아직 이전 규칙을 따르며 별도로 이전합니다.
- 경로 변경이 포함된 릴리스는 BE와 FE를 같은 시점에 `main`으로 병합·배포하고 rollback도 함께 합니다.

### pagination

- `page`는 1부터, `size`는 1 이상 100 이하(기본 20). 범위를 벗어나면 400입니다.
- 알림 본인 목록은 커서 방식(`beforeId`, `size` 1~50)입니다.
- 목록 응답: `items, page, size, totalElements, totalPages, hasNext` (벌점·알림 관리자 이력 기준).

### 시간

- instant는 UTC ISO-8601(`Z`)로 주고받습니다. 입력 시각은 밀리초로 정규화합니다.

## 3. 엔드포인트

### 인증 (`/auth`)

| Method | Endpoint | 설명 |
|---|---|---|
| `GET` | `/auth/discord_login?code=` | Discord OAuth callback. 세션 발급 후 `FRONT_REDIRECT_URI`로, 가입되지 않은 사용자는 `REGISTER_URI`로 redirect |
| `GET` | `/auth/me` | 현재 사용자 정보 (Discord 조회 실패 시 503) |
| `GET` | `/auth/guilds` | 사용자가 속한 채팅방 목록 (Discord 조회 실패 시 저장된 목록으로 대체) |
| `POST` | `/auth/guild/select` | 본문 `{"guildId": "..."}`. 채팅방 멤버십 확인 후 `botId`가 담긴 토큰 재발급 |
| `POST` | `/auth/refresh` | RefreshToken으로 AccessToken 재발급 |
| `GET` | `/auth/check` | 로그인 상태와 `roomSelected` 여부 |
| `POST` | `/auth/logout` | 세션 폐기, 쿠키 만료 |

### 메뉴용 모듈 설정 (`/api/bots/current/module-configs`)

| Method | Endpoint | 권한 | 설명 |
|---|---|---|---|
| `GET` | `/api/bots/current/module-configs` | 채팅방 선택을 마친 인증 회원 | 현재 JWT의 `botId`로 config DB `module_config` 조회 |

- 요청 본문·path/query 식별자는 없습니다. 클라이언트가 전달한 `botId`·`bot_id`는 사용하지 않고 `GuildContext.requireBotId()`만 조회 범위로 씁니다.
- 성공: `ApiResponse`의 `response`는 `[{moduleId, addOns}]` 배열. `moduleId` 오름차순이며 `chatbot`, `network_operations`, `shadowverse` 세 종류만 조회하므로 복합 PK 기준 최대 3행입니다. pagination은 없습니다. 설정이 없으면 `200`, `response: []`입니다.
- `chatbot`·`shadowverse`는 해당 모듈 행의 존재로 활성화합니다. `network_operations`는 원본 JSON `config.add_on.academy`·`config.add_on.competition`이 객체일 때 해당 이름을 `addOns`에 포함합니다(빈 객체도 포함). 누락·null·다른 타입은 활성화하지 않습니다.
- 응답에는 원본 config, `botId`, 채널·역할·회원 정보와 기타 설정을 넣지 않습니다. `addOns`는 항상 배열이며 아카데미·대회 이외의 부가 기능은 포함하지 않습니다.
- FE는 이 응답을 받은 뒤 활성화된 아카데미·대회에 한해서 기존 권한 API를 조회합니다. 모듈 활성 여부는 사용자의 기능 권한을 대신하지 않습니다.
- 미인증·유효하지 않은 토큰·채팅방 미선택·해당 채팅방 비회원은 기존 `/api/**` 규칙대로 `401`입니다.
- 배포: BE 신규 API를 먼저 배포한 뒤 FE를 배포합니다. FE에서 조회 실패 시 해당 모듈 메뉴를 숨기고 재시도를 제공합니다. DB migration은 없습니다. 근거: [ADR-0005](adr/0005-module-config-menu.md).

### Academy (`/api/academy`)

| Method | Endpoint | 권한 | 설명 |
|---|---|---|---|
| `GET` | `/me/permissions` | 인증 | 본인 Academy 역할 |
| `GET` | `/` | 인증 | Academy 목록 |
| `GET` | `/{academyId}/classes` | 인증 | 반 목록 |
| `GET` | `/{academyId}/subjects` | 인증 | 과목 목록 |
| `GET` | `/lesson-records?date=&time=&subject=&teacherId=&academyId=&classId=` | 교사 이상 | 수업 기록 목록 (**배열 직접 반환**) |
| `POST` | `/lesson-record` | 교사 이상 | 수업 기록 작성 |
| `PUT` | `/lesson-record/{id}` | ADMIN / 학원장 / 주 담당 교사 | 수업 기록 수정 |
| `DELETE` | `/lesson-record/{id}` | ADMIN / 학원장 / 주 담당 교사 | 수업 기록 삭제 |
| `GET` | `/students?academyId=&classId=&academyMemberId=&status=&page=&size=` | 교사 이상(해당 Academy) | 학생 현황 |
| `GET` | `/students/options?academyId=&classId=` | 인증 | 학생 현황 필터 옵션 |
| `GET` | `/students/{academyId}/classes/{classId}` | ADMIN / 학원장 / 해당 반 교사 | 반 학생 목록 |
| `GET` | `/teachers?academyId=&classId=&academyMemberId=&status=&page=&size=` | 학원장(해당 Academy) | 강사 현황 |
| `GET` | `/teachers/options?academyId=&classId=` | 인증 | 강사 현황 필터 옵션 |
| `GET` | `/teachers/{academyId}/classes/{classId}` | 해당 Academy 구성원 | 반 강사 목록 |

Academy 권한은 채팅방 단위 식별자 `sk`(=`(botId, discordId)`)로 판정합니다. `academyId` 없이 학생·강사 현황을 조회하면 "어느 Academy에서든" 해당 역할인지 확인합니다.

### ChatBot 저장소 (`/api/repository`)

| 도메인 | Method | Endpoint | 설명 |
|---|---|---|---|
| Content | `GET` | `/content/list` | 추천 콘텐츠 목록 (`page`, `size`) |
| | `POST` | `/content/save_all` | 일괄 저장 |
| | `POST` | `/content/delete_all` | 일괄 삭제 |
| Learning | `GET` / `POST` / `POST` | `/learning/list`, `/learning/save_all`, `/learning/delete_all` | 학습 자료 |
| Menu | `GET` / `POST` / `POST` | `/menu/list`, `/menu/save_all`, `/menu/delete_all` | 메뉴 추천 |
| Music | `GET` / `POST` / `POST` | `/music/list`, `/music/save_all`, `/music/delete_all` | 음악 추천 |

### 포인트

| Method | Endpoint | 권한 | 설명 |
|---|---|---|---|
| `GET` | `/api/repository/membership/point_log?page=1&size=20` | 인증 | 본인 포인트 내역 |
| `GET` | `/api/admin/points/members?discordId=&page=1&size=20` | ADMIN | 재적 회원을 Discord ID로 검색 |
| `GET` | `/api/admin/points/members/{discordId}?page=1&size=20` | ADMIN | 보유 포인트와 내역 |
| `POST` | `/api/admin/points/members/{discordId}/transactions` | ADMIN | 입금·출금 후 최신 상세 반환 |
| `PATCH` | `/api/admin/points/members/{discordId}/logs/{originalAmount}?at={timestamp}` | ADMIN | 내역 금액·설명 수정 |
| `DELETE` | `/api/admin/points/members/{discordId}/logs/{originalAmount}?at={timestamp}` | ADMIN | 내역 삭제와 잔액 롤백 |

- 관리자 API는 `state='재적'`인 Discord 회원만 대상으로 합니다. 이전 경로 `/api/repository/membership/admin/points/**`는 제거되었습니다(404).
- 거래 본문: `type`(`DEPOSIT`|`WITHDRAW`), 양수 `amount`, 255자 이하 `description`. 잔액 변경과 `PayLog` 기록은 하나의 transaction이며, 잔액보다 큰 출금은 409입니다.
- 내역 수정은 `amount`, `description` 중 하나 이상. 금액 수정 시 `새 금액 - 기존 금액`만큼 잔액을 보정하고, 삭제 시 해당 금액만큼 잔액을 롤백합니다.
- 입·출금이 성공하면 대상 회원에게 `POINT` 개인 알림이 같은 transaction에서 발행됩니다.
- 입·출금 요청에는 요청 ID가 없어, 결과를 모르는 채 재전송하면 중복 반영될 수 있습니다.

### 벌점 ([ADR-0003](adr/0003-penalty-log.md), [ADR-0006](adr/0006-penalty-manager-role.md))

권한: 현재 채팅방 `RoleTable`에 `운영 매니저` 또는 `운영 본부원`이 **들어간** 역할이 있거나 서버장(`ROLE_ADMIN`)이면 관리 API를 쓸 수 있습니다(`@penaltyAuth.isManager`). 권한이 없으면 공통 규칙대로 404입니다.

| Method | Endpoint | 설명 |
|---|---|---|
| `GET` | `/api/penalties/me/permissions` | 로그인한 회원 누구나. `{manager}` — 화면의 벌점 관리 메뉴 표시용(최종 판단은 서버) |
| `POST` | `/api/penalties` | 벌점 부여, 같은 `requestId` 재전송 처리 |
| `GET` | `/api/penalties` | 채널·대상 필터 이력 |
| `GET` | `/api/penalties/members` | 재적 회원 최근 30일 누적 순위 |
| `GET` | `/api/penalties/members/{discordId}` | 대상자 누적과 이력 상세 |
| `PATCH` | `/api/penalties/{penaltyId}/cancel` | 취소(이력 보존, 누적에서 제외) |
| `GET` | `/api/me/penalties` | 본인 누적과 이력 |

- 호환 경로: `/api/admin/penalties/**`는 구버전 FE를 위해 같은 handler로 남겨 두었으며 `/api/admin/**` URL 규칙 때문에 서버장만 통과합니다. 새 FE 배포 후 호출이 없음을 확인하면 제거합니다(ADR-0006).

- 모든 벌점은 현재 토큰의 `botId`로 제한하고, 대상은 `DiscordUsers`의 재적 회원 `(botId, discordId)`입니다. `Users.sk` 발급은 요구하지 않습니다.
- 부여 본문: `requestId`(UUID), `targetDiscordId`, `channelId`, 선택 `channelName`, `reason`, `score=1`, 선택 `occurredAt`. 발생 시각을 생략하면 서버 현재 UTC 시각, 미래 시각은 400입니다. 같은 `requestId`·같은 내용은 현재 상세(이력 첫 페이지)를 반환하고, 다른 내용은 409입니다.
- 이력 쿼리: `channelId`, `discordId`(정확히 일치), `sort=OCCURRED_AT_DESC|OCCURRED_AT_ASC`, `page`, `size`. 같은 발생 시각은 `id`로 정렬합니다. 순위 쿼리의 `discordId`는 부분 검색입니다.
- 취소 본문: 필수 `reason`(255자 이하). 이미 취소한 내역을 다시 취소하면 기존 감사 정보를 유지합니다. 다른 길드의 ID나 비재적 대상은 404입니다.
- 대상 상세: `discordId, username, state, cumulativeScore30d, history`. 이력 item: `penaltyId, channelId, channelName, targetDiscordId, targetUsername, reason, score, issuerDiscordId, occurredAt, createdAt, status, targetCumulativeScore30d, canceledByDiscordId, canceledAt, cancellationReason`. 순위 item: `discordId, username, cumulativeScore30d, penaltyCount30d, lastOccurredAt`.

### 알림 ([ADR-0004](adr/0004-notification.md))

모든 알림은 채팅방(`botId`) 단위입니다.

**회원 본인** (`/api/me/notifications`)

| Method | Endpoint | 설명 |
|---|---|---|
| `GET` | `?beforeId=&size=20` | 채팅방 전체 + 본인 대상 알림을 최신순 커서 페이지로 조회 (`size` 1~50) |
| `GET` | `/unread-count` | 읽지 않은 개수, 최신 알림 ID, 현재 읽음 위치 |
| `PUT` | `/read-cursor` | `{"lastReadId": n}`까지 읽음 처리. 멱등이며 뒤로 가지 않음 |
| `GET` | `/stream` | SSE(`text/event-stream`). `ready`(읽지 않은 요약) → `notification`(새 알림) |

**관리자** (`/api/admin/notifications`, `ROLE_ADMIN`)

| Method | Endpoint | 설명 |
|---|---|---|
| `POST` | `/` | 로그인한 채팅방에 발행. `requestId` 필수 |
| `GET` | `?page=1&size=20` | 발행 이력 최신순 |

**외부 시스템** (`/api/integrations/notifications`, `X-Api-Key`)

| Method | Endpoint | 설명 |
|---|---|---|
| `POST` | `/` | 본문의 `botId`로 발행. API Key에 허용된 botId만 가능(아니면 403) |

- 발행 본문: `requestId`(영문·숫자·`._:-` 64자 이하), `targetType`(`GUILD`|`USER`), `targetDiscordId`(USER일 때만), `category`(`ANNOUNCEMENT`|`EVENT`|`POINT`|`SYSTEM`), `title`(100자), `body`(1000자), `link`(선택, `/`로 시작하는 앱 내부 경로).
- 같은 `requestId`·다른 내용은 409, USER 대상이 채팅방 재적 회원이 아니면 404입니다.
- 내부 기능: `NotificationPublisher.publish(NotificationCommand.internal(...))`. 호출자 transaction에 참여합니다.

### 대회 (`/api/competitions`, 대회 매니저)

권한: 현재 채팅방 `RoleTable`에 `대회 매니저`가 **들어간** 역할(예: `섀버 대회 매니저`)이 있거나 서버장(`ROLE_ADMIN`)이면 쓸 수 있습니다(`@competitionAuth.isManager`, 아카데미의 `@academyAuth`와 같은 방식). 서버장 전용이 아니므로 `/api/admin/**` 밖에 둡니다. 권한이 없으면 공통 규칙대로 404입니다.

WebUI_BE는 로그인한 채팅방(`botId`)의 대회 게시판에 **봇 계정으로 평문 공지만 게시**합니다. 대회 등록(스케줄러), 참가 이모지·참가자 역할·대회방 생성, WebUI 알림 발행은 빗자루 봇이 게시판 글을 감지해 처리합니다.

| Method | Endpoint | 설명 |
|---|---|---|
| `GET` | `/me/permissions` | 로그인한 회원 누구나. `{manager}` — 화면의 대회 메뉴 표시용(최종 판단은 서버) |
| `GET` | `/boards` | 게시판 목록 `[{key, channelId, name, joinable}]`. `joinable`은 참가 이모지 등이 자동 생성되는 게시판(`inner_board`) |
| `POST` | `/notices/preview` | 게시하지 않고 검증과 평문 조립만 함 → `{content}` |
| `POST` | `/notices` | `{requestId, boardKey, notice}` 게시 → `{boardKey, channelId, messageId, messageUrl, content}` |
| `POST` | `/winners` | 우승 칭호 부여 `{competitionName, version, winnerDiscordId, acquisition}` → `{competitionName, version, winnerDiscordId, winnerName, acquisition}` |
| `GET` | `/winners?page=1&size=20` | 현재 채팅방 칭호 부여 이력(대회 날짜 최신순) |

- `notice` 본문: `title`(100자, 큰따옴표 불가), `participantWay`, `format`, `registrationStart`, `registrationEnd`, `eventStart`(UTC ISO-8601 `2026-10-02T13:00:00Z`. 게시글에는 한국 시간으로 분 단위까지 적음), 선택 `prizes[{rank, content}]`, 선택 `extraFields[{key, value}]`(각 10개 이하).
- 봇 파서(`InfoExtractor`) 규칙에 맞게 검증합니다. 모든 값은 한 줄이어야 하고, `참가 방법 : `·`접수 기간 : `·`진행 기간 : ` 표시와 `개최되었습니다`를 넣을 수 없습니다. 추가 입력란 이름은 예약어(`참가 방법`, `진행 형식`, `접수 기간`, `진행 기간`, `우승 상품`)나 `:`를 쓸 수 없고 중복될 수 없습니다. 접수 시작 < 접수 마감 ≤ 진행 시작이어야 하고, 접수 마감은 현재보다 뒤여야 합니다. 전체 2000자 이하입니다. 어기면 400입니다.
- 진행 기간은 항상 `시작 ~ 종료시까지`로 게시합니다. 봇 파서가 진행 기간 줄에서 날짜를 하나만 읽기 때문입니다.
- 게시판 목록은 config DB `module_config(botId, network_operations)`의 `add_on.competition.notice_channel.boards`(`{키: 채널 ID}`)에서, 봇 토큰은 `bot_env.discord_token`에서 읽습니다. 설정이 없으면 404, 디스코드 게시 실패는 502입니다.
- 멘션은 모두 비활성화(`allowed_mentions.parse=[]`)해 입력의 `@everyone` 등이 발동하지 않습니다.
- 우승 칭호(`Competition.Winners`, [migration 0006](migrations/0006_competition_winners_bot_id.sql)): `version`은 `GameVersion` 값(`s1`·`s2`, WebUI_FE `GameVersionType`과 같음), `acquisition`은 대회 개최 날짜(`2026-10-02`, 시각이 아닌 한국 날짜라 UTC로 바꾸지 않음, 오늘 이후 불가), `winnerDiscordId`는 현재 채팅방 재적 회원(아니면 404). 같은 채팅방·대회명·버전·회원이 이미 있으면 409입니다.

## 4. idempotency 요약

| Endpoint | key | 같은 key 재전송 |
|---|---|---|
| `POST /api/penalties` (호환: `/api/admin/penalties`) | `requestId`(UUID), 길드 단위 | 같은 내용이면 현재 상세, 다른 내용이면 409 |
| `POST /api/admin/notifications`, `POST /api/integrations/notifications` | `requestId`, `(botId, source, sourceRef)` 단위 | 같은 내용이면 기존 결과(재전달 없음), 다른 내용이면 409 |
| `PUT /api/me/notifications/read-cursor` | 요청 자체가 멱등 | 읽음 위치는 뒤로 가지 않음 |
| `POST /api/competitions/notices` | `requestId`, 채팅방 단위 (Discord `nonce`) | 몇 분 안의 재전송은 디스코드가 기존 메시지를 반환. 그 이후 재전송은 중복 게시될 수 있음 |
| `POST /api/competitions/winners` | (bot_id, 대회명, 버전, 우승자) PK | 이미 부여됐으면 409(재부여 없음) |
| `POST /api/admin/points/members/{discordId}/transactions` | **없음** | 중복 반영 가능 (후속 작업) |

key는 해당 행이 DB에 남아 있는 동안 유효합니다. 벌점 기록은 운영자가 삭제하기 전까지 보존하고, 알림 보존 기간은 미정입니다.

### 봇 설정 리소스 계약 (2026-09-30)

`GET /api/bots/current/module-configs`의 `current`는 인증된 요청에서 선택된 JWT `botId`를 뜻한다. 모듈 설정의 소유자는 사용자(`me`)가 아닌 봇/채팅방이다. 인증·재적 여부는 접근 조건이며, 조회 결과는 오직 `botId`로 결정된다. 같은 봇의 사용자·역할이 달라도 설정 응답은 같으며, 다른 봇의 설정은 섞이지 않는다. 사용자별 아카데미·대회 권한은 기존 권한 API에서 별도로 조회한다.

기존 `/api/me/module-configs`는 미병합 초안 경로로 폐기하며 호환 alias를 제공하지 않는다. BE의 새 경로를 먼저 배포한 뒤 FE를 배포한다.
