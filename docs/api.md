## 📱 API 엔드포인트

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