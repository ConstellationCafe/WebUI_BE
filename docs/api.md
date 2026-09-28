## 📱 API 엔드포인트

### 공통 pagination 규칙

- `page`는 1부터 시작합니다.
- `size`는 1 이상 100 이하입니다.
- 범위를 벗어난 값은 `400 Bad Request`로 응답합니다.

### 🔑 인증 API (`/auth`)
| Method | Endpoint | 설명 |
|--------|----------|------|
| `GET` | `/auth/discord_login` | Discord OAuth 로그인 |
| `GET` | `/auth/me` | 현재 사용자 정보 |
| `POST` | `/auth/refresh` | 토큰 갱신 |
| `GET` | `/auth/check` | 로그인 상태 확인 |
| `POST` | `/auth/logout` | 로그아웃 |

### 📚 콘텐츠 API (`/api/repository`)
| 도메인 | Endpoint | 설명 |
|--------|----------|------|
| **Content** | `/content/list` | 추천 콘텐츠 목록 |
| | `/content/save_all` | 콘텐츠 일괄 저장 |
| | `/content/delete_all` | 콘텐츠 일괄 삭제 |
| **Learning** | `/learning/list` | 학습 자료 목록 |
| | `/learning/save_all` | 학습 자료 일괄 저장 |
| | `/learning/delete_all` | 학습 자료 일괄 삭제 |
| **Menu** | `/menu/list` | 메뉴 추천 목록 |
| | `/menu/save_all` | 메뉴 일괄 저장 |
| | `/menu/delete_all` | 메뉴 일괄 삭제 |
| **Music** | `/music/list` | 음악 추천 목록 |
| | `/music/save_all` | 음악 일괄 저장 |
| | `/music/delete_all` | 음악 일괄 삭제 |

### 👥 멤버십 API
| Method | Endpoint | 설명 |
|--------|----------|------|
| `GET` | `/api/repository/membership/point_log?page=1&size=20` | 본인 포인트 내역 조회 |

> 이전 문서의 `/membership/list`, `/membership/save_all`은 활성 엔드포인트가 아니어서 제거했습니다.

### 🧭 경로 규칙 (ADR-0002)

- 관리자 전용 API는 모두 `/api/admin/**` 아래에 둡니다. `SecurityConfig`가 이 경로 전체를 `ROLE_ADMIN`으로 막고, 컨트롤러의 `@PreAuthorize`로 한 번 더 확인합니다.
- 인증은 됐지만 관리자가 아닌 사용자가 `/api/admin/**`을 호출하면 `404 Not Found`(`NOT_FOUND` 래퍼)를 받습니다. 인증이 안 된 요청은 기존처럼 `401`입니다.
- 새 API의 경로는 kebab-case와 복수형 명사를 쓰고, 동작은 HTTP method로 표현합니다. 내부 계층·패키지 이름(`repository`, `erp` 등)은 경로에 넣지 않습니다.
- 버전 접두사(`/v1`)는 두지 않습니다. 외부 클라이언트에 공개할 때 도입합니다.
- 기존 일반 API(`/api/repository/...`, `/api/academy/...`)는 이번 변경 범위가 아니며 별도로 이전합니다.

### 🪙 관리자 포인트 API (`/api/admin/points`)

> 2026-09-28: `/api/repository/membership/admin/points`에서 이전했습니다(ADR-0002). 이전 경로는 제거되어 더 이상 응답하지 않습니다(`404`).

모든 API는 `ROLE_ADMIN` 권한이 필요하며, `state='재적'`인 Discord 회원만 대상으로 합니다.

| Method | Endpoint | 설명 |
|--------|----------|------|
| `GET` | `/members?discordId=&page=1&size=20` | 재적 회원을 Discord ID로 검색하고 페이지 조회 |
| `GET` | `/members/{discordId}?page=1&size=20` | 회원의 보유 포인트와 포인트 내역 조회 |
| `POST` | `/members/{discordId}/transactions` | 포인트 입금 또는 출금 후 최신 상세 반환 |
| `PATCH` | `/members/{discordId}/logs/{originalAmount}?at={timestamp}` | 포인트 내역의 금액 또는 설명 수정 후 최신 상세 반환 |
| `DELETE` | `/members/{discordId}/logs/{originalAmount}?at={timestamp}` | 포인트 내역 삭제 및 잔액 롤백 후 최신 상세 반환 |

거래 요청 본문은 `type`(`DEPOSIT` 또는 `WITHDRAW`), 양수 `amount`, 255자 이하 `description`을 사용합니다. 잔액 변경과 `PayLog` 기록은 하나의 트랜잭션으로 처리하며 잔액보다 큰 출금은 `409 Conflict`로 거부합니다.

내역 수정 요청은 `amount`, `description` 중 하나 이상을 사용합니다. 금액 수정 시 `새 금액 - 기존 금액`만큼 `CoinTable` 잔액을 보정하고, 삭제 시 `현재 잔액 - 삭제 내역 금액`으로 롤백합니다. 내역 변경과 잔액 보정은 하나의 트랜잭션으로 처리됩니다.

### 벌점 API

상세 계약은 Notion `/명세서/API 명세서/Penalty API 명세`에서 관리합니다. 모든 벌점은 현재 토큰의 길드(`botId`)로 제한하고 UTC ISO-8601(`Z`)로 반환합니다. 대상은 `(botId, discordId)`로 식별하는 `DiscordUsers`의 재적 회원이며 `Users.sk` 발급은 요구하지 않습니다. 관리자 경로는 `ROLE_ADMIN`만 사용하며 비관리자에게는 ADR-0002 규칙에 따라 404를 반환합니다. `page` 기본값은 1, `size` 기본값은 20입니다.

| Method | Endpoint | 설명 |
|--------|----------|------|
| `POST` | `/api/admin/penalties` | 벌점 부여, 동일 `requestId` 재전송 처리 |
| `GET` | `/api/admin/penalties` | 채널·대상 필터가 있는 이력 페이지 |
| `GET` | `/api/admin/penalties/members` | 재적 회원의 최근 30일 누적 순위 |
| `GET` | `/api/admin/penalties/members/{discordId}` | 대상자의 누적 및 이력 상세 |
| `PATCH` | `/api/admin/penalties/{penaltyId}/cancel` | 취소 이력 보존, 누적에서 제외 |
| `GET` | `/api/me/penalties` | 인증된 회원 본인의 누적 및 이력 상세 |

부여 본문: `requestId`(UUID), `targetDiscordId`, `channelId`, 선택 `channelName`, `reason`, `score=1`, 선택 `occurredAt`. 발생 시각을 생략하면 서버 현재 UTC 시각을 사용하며 미래 시각은 400입니다. 같은 길드의 같은 `requestId`를 같은 내용으로 재전송하면 현재 대상 상세(이력 첫 페이지)를 반환하고, 내용이 다르면 409입니다. 입력 시각은 밀리초로 정규화합니다.

이력 쿼리는 `channelId`, `discordId`(정확히 일치), `sort=OCCURRED_AT_DESC|OCCURRED_AT_ASC`, `page`, `size`입니다. 동일 발생 시각은 `id`로 정렬합니다. 순위 쿼리의 `discordId`는 부분 검색입니다. 취소 본문은 필수 `reason`(255자 이하)이고 응답은 대상 최신 상세입니다. 이미 취소한 내역을 다시 취소하면 기존 감사 정보가 유지됩니다. 다른 길드의 ID나 비재적 대상 상세는 404입니다.

목록 응답은 `items, page, size, totalElements, totalPages, hasNext`, 대상 상세는 `discordId, username, state, cumulativeScore30d, history` 구조입니다. 이력 item에는 `penaltyId, channelId, channelName, targetDiscordId, targetUsername, reason, score, issuerDiscordId, occurredAt, createdAt, status, targetCumulativeScore30d, canceledByDiscordId, canceledAt, cancellationReason`이 포함됩니다. 순위 item은 `discordId, username, cumulativeScore30d, penaltyCount30d, lastOccurredAt`입니다. `history`도 같은 페이지 구조입니다. 공통 `ApiResponse(success, response, error)`로 감싸고 성공 시 200입니다. DB 변경은 [0003 migration](migrations/0003_penalty.sql), [0004 migration](migrations/0004_penalty_discord_identity.sql), [ADR-0003](adr/0003-penalty-log.md)을 참고합니다.
