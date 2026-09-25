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

### 🪙 관리자 포인트 API (`/api/repository/membership/admin/points`)

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
