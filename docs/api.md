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
| **Menu** | `/menu/list` | 메뉴 추천 목록 |
| | `/menu/save_all` | 메뉴 일괄 저장 |
| **Music** | `/music/list` | 음악 추천 목록 |
| | `/music/save_all` | 음악 일괄 저장 |

### 👥 멤버십 API
| Method | Endpoint | 설명 |
|--------|----------|------|
| `GET` | `/membership/list` | 멤버십 정보 목록 |
| `POST` | `/membership/save_all` | 멤버십 정보 저장 |

### 🧭 경로 규칙 (ADR-0002)

- 관리자 전용 API는 모두 `/api/admin/**` 아래에 둡니다. `SecurityConfig`가 이 경로 전체를 `ROLE_ADMIN`으로 막고, 컨트롤러의 `@PreAuthorize`로 한 번 더 확인합니다.
- 인증은 됐지만 관리자가 아닌 사용자가 `/api/admin/**`을 호출하면 `404 Not Found`(`NOT_FOUND` 래퍼)를 받습니다. 인증이 안 된 요청은 기존처럼 `401`입니다.
- 새 API의 경로는 kebab-case와 복수형 명사를 쓰고, 동작은 HTTP method로 표현합니다. 내부 계층·패키지 이름(`repository`, `erp` 등)은 경로에 넣지 않습니다.
- 버전 접두사(`/v1`)는 두지 않습니다. 외부 클라이언트에 공개할 때 도입합니다.
- 기존 일반 API(`/api/repository/...`, `/api/academy/...`)는 이번 변경 범위가 아니며 별도로 이전합니다.

### 🪙 관리자 포인트 API (`/api/admin/points`)

> 2026-09-28: `/api/repository/membership/admin/points`에서 이전했습니다(ADR-0002). 이전 경로는 BE/FE 배포 순서가 어긋나도 깨지지 않도록 **호환 경로로 잠시 유지**하며, 새 경로를 쓰는 WebUI_FE가 운영에 배포된 뒤 제거합니다. 신규 호출은 반드시 `/api/admin/points`를 사용합니다.

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

### 🔔 알림 API (ADR-0004)

상세 계약은 Notion `/명세서/API 명세서/Notification API 명세`에서 관리합니다. 모든 알림은 채팅방(`botId`) 단위이며 시간은 UTC ISO-8601(`Z`)입니다. 응답은 공통 `ApiResponse(success, response, error)`로 감쌉니다. DB 변경은 [0005 migration](migrations/0005_notification.sql)을 참고합니다.

**회원 본인** (`/api/me/notifications`, 로그인 완료 토큰 필요)

| Method | Endpoint | 설명 |
|--------|----------|------|
| `GET` | `?beforeId=&size=20` | 채팅방 전체 + 본인 대상 알림을 최신순 커서 페이지로 조회 (`size` 1~50) |
| `GET` | `/unread-count` | 읽지 않은 개수, 최신 알림 ID, 현재 읽음 위치 |
| `PUT` | `/read-cursor` | `{"lastReadId": n}`까지 읽음 처리. 멱등이며 뒤로 가지 않음 |
| `GET` | `/stream` | SSE(`text/event-stream`). `ready`(읽지 않은 요약) → `notification`(새 알림) |

**관리자** (`/api/admin/notifications`, `ROLE_ADMIN`, 비관리자는 404)

| Method | Endpoint | 설명 |
|--------|----------|------|
| `POST` | `/` | 로그인한 채팅방에 발행. `requestId` 필수(재전송 멱등) |
| `GET` | `?page=1&size=20` | 발행 이력 최신순 (`size` 1~100) |

**외부 시스템** (`/api/integrations/notifications`, `X-Api-Key` 헤더)

| Method | Endpoint | 설명 |
|--------|----------|------|
| `POST` | `/` | 본문의 `botId`로 발행. API Key에 허용된 botId만 가능(아니면 403) |

발행 본문: `requestId`(영문·숫자·`._:-` 64자 이하), `targetType`(`GUILD`|`USER`), `targetDiscordId`(USER일 때만), `category`(`ANNOUNCEMENT`|`EVENT`|`POINT`|`SYSTEM`), `title`(100자), `body`(1000자), `link`(선택, `/`로 시작하는 앱 내부 경로). 같은 `requestId`·다른 내용은 `409`, USER 대상이 채팅방 재적 회원이 아니면 `404`입니다.

**내부 기능**: `NotificationPublisher.publish(NotificationCommand.internal(...))`. 호출자 트랜잭션에 참여합니다. 관리자 포인트 입·출금 시 대상 회원에게 `POINT` 개인 알림이 발행됩니다.
