## 📱 API 엔드포인트

### 공통 pagination 규칙

- \`page\`는 1부터 시작합니다.
- \`size\`는 1 이상 100 이하입니다.
- 범위를 벗어난 값은 \`400 Bad Request\`로 응답합니다.

### 🔑 인증 API (\`/auth\`)
| Method | Endpoint | 설명 |
|--------|----------|------|
| \`GET\` | \`/auth/discord_login\` | Discord OAuth 로그인 |
| \`GET\` | \`/auth/me\` | 현재 사용자 정보 |
| \`POST\` | \`/auth/refresh\` | 토큰 갱신 |
| \`GET\` | \`/auth/check\` | 로그인 상태 확인 |
| \`POST\` | \`/auth/logout\` | 로그아웃 |

### 📚 콘텐츠 API (\`/api/repository\`)
| 도메인 | Endpoint | 설명 |
|--------|----------|------|
| **Content** | \`/content/list\` | 추천 콘텐츠 목록 |
| | \`/content/save_all\` | 콘텐츠 일괄 저장 |
| | \`/content/delete_all\` | 콘텐츠 일괄 삭제 |
| **Learning** | \`/learning/list\` | 학습 자료 목록 |
| | \`/learning/save_all\` | 학습 자료 일괄 저장 |
| **Menu** | \`/menu/list\` | 메뉴 추천 목록 |
| | \`/menu/save_all\` | 메뉴 일괄 저장 |
| **Music** | \`/music/list\` | 음악 추천 목록 |
| | \`/music/save_all\` | 음악 일괄 저장 |

### 👥 Membership API

| Method | Endpoint | 설명 | 권한 |
|--------|----------|------|------|
| \`GET\` | \`/api/repository/membership/point_log\` | 로그인 사용자의 포인트 내역 조회 | 인증 |
| \`GET\` | \`/api/repository/membership/admin/points/members\` | 재적 회원 포인트 목록 조회, Discord ID 완전 일치 검색, 페이지네이션 | 관리자 |
| \`GET\` | \`/api/repository/membership/admin/points/members/{discordId}\` | 선택 회원의 현재 잔액과 포인트 내역 조회 | 관리자 |
| \`POST\` | \`/api/repository/membership/admin/points/members/{discordId}/transactions\` | 포인트 입금 또는 출금 및 설명 기록 | 관리자 |

관리자 포인트 목록의 \`page\`는 1부터 시작하며, \`size\`는 1~100입니다. 목록에는 \`state='재적'\`인 사용자만 포함됩니다. 포인트 조정 요청은 \`type\`(\`DEPOSIT\` 또는 \`WITHDRAW\`), 양수 \`amount\`, 필수 \`description\`(최대 1000자)을 받습니다. 잔액 갱신과 PayLog 기록은 하나의 트랜잭션으로 처리되며, 잔액보다 큰 출금은 \`409 Conflict\`로 거절됩니다.
