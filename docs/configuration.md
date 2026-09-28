# 설정과 환경

> 상태: Active  
> 마지막 검토일: 2026-09-28  
> 상위 문서: [README](../README.md) · 관련: [deploy](deploy.md), [운영](operations.md)

비밀값은 저장소에 커밋하지 않습니다. local은 셸·IDE 환경 변수나 커밋되지 않는 `.env`, production은 GitHub Actions secrets로 주입합니다. 이 문서에는 변수 이름·의미·안전한 예시 형식만 적습니다.

## 1. 환경 변수

| 변수 | 필수 | 용도 | 안전한 예시 형식 |
|---|:---:|---|---|
| `SPRING_PROFILES_ACTIVE` | 예 | 실행 profile. Compose 기본값 `prod` | `dev` 또는 `prod` |
| `DB_URL` | 예 | 주 DB(`constellation`) JDBC URL | `jdbc:mysql://db:3306/database` |
| `DB_CONFIG_URL` | 예 | 설정 DB(`config_db`) JDBC URL | `jdbc:mysql://db:3306/config` |
| `DB_USER` | 예 | 두 datasource 공용 사용자 | `service_user` |
| `DB_PASSWORD` | 예 | DB 비밀값 | secret manager에서 주입 |
| `JWT_SECRET` | 예 | JWT 서명 비밀값 | 충분한 길이의 무작위 값 |
| `DISCORD_CLIENT_ID` | 예 | Discord OAuth client ID | Discord 발급 값 |
| `DISCORD_CLIENT_SECRET` | 예 | Discord OAuth secret | secret manager에서 주입 |
| `DISCORD_REDIRECT_URI` | 예 | OAuth callback URI | `https://example.test/auth/discord_login` |
| `DISCORD_TOKEN_URI` | 예 | Discord token endpoint | HTTPS URI |
| `DISCORD_USER_URI` | 예 | Discord user endpoint | HTTPS URI |
| `DISCORD_GUILDS_URI` | 예 | Discord guilds endpoint | HTTPS URI |
| `FRONT_REDIRECT_URI` | 예 | 로그인 후 이동할 frontend URI. CORS 허용 origin으로도 사용 | `https://example.test` |
| `REGISTER_URI` | 예 | 채팅방 미가입자에게 안내할 가입 링크 | HTTPS URI |
| `SPRING_DATA_REDIS_HOST` | 아니요 | Redis host (기본 `redis`) | `redis` |
| `SPRING_DATA_REDIS_PORT` | 아니요 | Redis port (기본 `6379`) | `6379` |
| `INTEGRATION_CLIENTS_0_ID` | 아니요 | 외부 알림 발행 client 식별자(비밀 아님). 미설정 시 외부 발행 API는 모두 401 | `discord-bot` |
| `INTEGRATION_CLIENTS_0_KEYSHA256` | client 설정 시 | client API Key의 SHA-256 hex(소문자 64자). **원문 키는 넣지 않음** | secret manager에서 주입 |
| `INTEGRATION_CLIENTS_0_BOTIDS` | client 설정 시 | client가 발행할 수 있는 botId(쉼표 구분) | `123456789012345678` |
| `NOTIFICATION_REALTIME_EMITTERTIMEOUTMILLIS` | 아니요 | SSE 연결 최대 유지 시간 (기본 30분) | `1800000` |
| `NOTIFICATION_REALTIME_HEARTBEATMILLIS` | 아니요 | SSE heartbeat 주기 (기본 25초) | `25000` |
| `NOTIFICATION_REALTIME_MAXCONNECTIONSPERUSER` | 아니요 | 회원당 SSE 연결 수 (기본 5) | `5` |
| `NOTIFICATION_REALTIME_MAXCONNECTIONS` | 아니요 | 인스턴스당 SSE 연결 수 (기본 1000) | `1000` |
| `NOTIFICATION_REALTIME_CHANNEL` | 아니요 | Redis Pub/Sub channel (기본 `webui:notifications`) | `webui:notifications` |

- 외부 client가 여러 개면 `_1_`, `_2_`처럼 번호를 늘립니다. 형식이 맞지 않으면 애플리케이션이 기동하지 않습니다.
- `docker-compose.yml`과 CD workflow에는 `INTEGRATION_CLIENTS_*`, `NOTIFICATION_REALTIME_*`이 아직 없습니다. 외부 발행을 켜려면 `webui_be.environment`와 `CD.yml`의 `env`·`envs`에 같은 이름으로 추가합니다.
- `kis.*`(`KisProperties`)는 bean만 있고 사용하지 않으므로 설정하지 않아도 됩니다.

## 2. 주입 방식

| 환경 | 방식 |
|---|---|
| local (`bootRun`) | 셸 export 또는 IDE run configuration |
| local (Compose) | 저장소 루트의 `.env` (커밋 금지, `.gitignore`·`.dockerignore` 대상) |
| production | GitHub Actions secrets → CD의 SSH step 환경 변수 → `docker compose` 변수 치환 |
| test | `application-test.yml` (H2 in-memory) |

## 3. profile별 차이

| 항목 | dev | prod |
|---|---|---|
| 주 DB `ddl-auto` | `update` | `validate` (schema 변경은 [수동 migration](migrations/README.md)으로만) |
| config DB `ddl-auto` | `none` | `none` |
| SQL 로그(`show_sql`) | 켬 | 끔 |
| Redis host / timeout | `redis` / 60초 | `${SPRING_DATA_REDIS_HOST:redis}` / 5초 |
| 쿠키 `Secure` | `false` | `true` (profile이 정확히 `prod`일 때만) |
| 포트 | 4003 | 4003 |

Spring의 환경 변수 우선순위에 따라 `SPRING_DATA_REDIS_HOST`·`SPRING_DATA_REDIS_PORT`는 두 profile 모두에서 yml 값을 덮어씁니다.

공통(`application.yml`): graceful shutdown, 종료 단계 제한 30초, actuator `health`·`prometheus` 노출, health 상세 비노출.

## 4. secret 관리

- production 비밀값은 GitHub Actions secrets에만 둡니다. 저장소의 `.env`에 의존하지 않습니다.
- rotation 주기·담당자, 접근 권한 승인·회수 방법은 아직 정하지 않았습니다([운영](operations.md#보안-운영)).
- 비밀값 노출이 의심되면 삭제에 그치지 않고 즉시 폐기·회전하고 이력과 영향 범위를 조사합니다.
