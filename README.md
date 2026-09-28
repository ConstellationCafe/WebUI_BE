# 빗자루 WebUI Backend

> 상태: Active  
> 적용 범위: `ConstellationCafe/WebUI_BE` 백엔드 서비스 (`develope` 기준)  
> 문서 담당자: 미정 — 프로젝트 책임자가 지정 필요  
> 마지막 검토일: 2026-09-28 (`develope` `291ee46`, PR #46~#50 병합 반영)

## 1. 프로젝트 개요

[빗자루](https://github.com/ConstellationCafe/DiscordBot)는 섀버 별자리 Cafe에서 운영하는 Discord 채팅 봇입니다. 이 저장소는 명령어 기반 봇 조작의 한계를 보완하는 WebUI([WebUI_FE](https://github.com/ConstellationCafe/WebUI_FE))가 호출하는 Spring Boot API 서버입니다.

주요 기능

- **인증**: Discord OAuth 2.0 로그인, JWT(HttpOnly 쿠키) + Redis 세션, 채팅방(`botId`) 선택 후 방 단위 권한 적용(ADR-0001)
- **ChatBot 저장소**: 콘텐츠·학습 자료·메뉴·음악 추천 데이터 조회와 일괄 저장·삭제
- **Academy**: 아카데미·반·과목, 학생·강사 현황, 수업 기록 작성·조회·수정·삭제
- **ERP 포인트**: 본인 포인트 내역, 관리자 포인트 입·출금과 내역 수정·삭제
- **ERP 벌점**: 관리자 벌점 부여·취소·이력·30일 누적 순위, 본인 벌점 조회(ADR-0003)
- **알림**: 채팅방 전체/회원 대상 알림 발행(관리자·내부 기능·외부 시스템), SSE 실시간 전달(ADR-0004)

기술 stack: Java 17, Spring Boot 3.4.3(Web, WebFlux, Data JPA, Security, Validation, Data Redis, Mail, Actuator), MySQL, Redis, JJWT 0.13.0, Micrometer Prometheus

## 2. 개발 환경

| 항목 | 요구사항 | 근거 |
|---|---|---|
| JDK | 17 (Temurin 권장) | `build.gradle` toolchain, `Dockerfile`, CI |
| Gradle | 9.7.1 Wrapper (`AuthServerPlatform/gradlew`) | `gradle-wrapper.properties` |
| MySQL | 8 이상, datasource 2개(`Constellation_Network` 주 DB, config DB) | `application-*.yml` |
| Redis | 7.4 (Compose는 `redis:7.4.2-alpine`) | `redis/Dockerfile` |
| Docker / Docker Compose | 컨테이너 실행·배포 시 | `Dockerfile`, `docker-compose.yml` |

지원 운영체제: JDK 17과 Docker가 동작하는 Linux·macOS·Windows. 운영 환경은 Linux 컨테이너(`eclipse-temurin:17-jre-jammy`, non-root `app` 사용자)입니다.

## 3. 설치·실행·검증

모든 Gradle 명령은 `AuthServerPlatform` 디렉터리에서 실행합니다.

```bash
cd AuthServerPlatform

# 로컬 실행 (4장의 환경 변수를 셸 또는 IDE에서 먼저 주입)
./gradlew bootRun --args='--spring.profiles.active=dev'

# 전체 검증: formatting(Spotless·EditorConfig), Checkstyle, 테스트, 배포 jar
./gradlew clean check bootJar --no-daemon
```

Docker Compose 실행(저장소 루트, 커밋되지 않는 `.env`에 값 주입):

```bash
docker compose config --quiet
docker compose up -d --build
docker compose ps
```

성공 기준

- `check bootJar`가 `BUILD SUCCESSFUL`로 끝나고 `build/libs/*.jar`가 생성됩니다.
- 실행 후 `GET http://localhost:4003/actuator/health/readiness`가 `UP`을 반환합니다.
- Compose는 `redis`가 healthy가 된 뒤 `webui_be`가 올라옵니다(포트 4003, Redis 6379).

## 4. 설정과 환경

비밀값은 저장소에 커밋하지 않습니다. local은 셸/IDE 환경 변수나 커밋되지 않는 `.env`, production은 GitHub Actions secrets로 주입합니다.

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
| `SPRING_DATA_REDIS_PORT` | 아니요 | Redis port, prod만 참조 (기본 `6379`) | `6379` |
| `INTEGRATION_CLIENTS_0_ID` | 아니요 | 외부 알림 발행 client 식별자(비밀 아님). 미설정 시 외부 발행 API는 모두 401 | `discord-bot` |
| `INTEGRATION_CLIENTS_0_KEYSHA256` | client 설정 시 | client API Key의 SHA-256 hex(소문자 64자). **원문 키는 넣지 않음** | secret manager에서 주입 |
| `INTEGRATION_CLIENTS_0_BOTIDS` | client 설정 시 | client가 발행할 수 있는 botId(쉼표 구분) | `123456789012345678` |
| `NOTIFICATION_REALTIME_*` | 아니요 | 실시간 알림 조정값. 기본값은 8장 참고 | `NOTIFICATION_REALTIME_MAXCONNECTIONS=1000` |

- client가 여러 개면 `_1_`, `_2_`처럼 번호를 늘립니다. 형식이 맞지 않으면 애플리케이션이 기동하지 않습니다.
- `docker-compose.yml`에는 `INTEGRATION_CLIENTS_*`, `NOTIFICATION_REALTIME_*`이 아직 없습니다. 외부 발행을 켜려면 `webui_be.environment`와 CD workflow의 `envs`에 같은 이름으로 추가해야 합니다.

환경별 차이

| 항목 | dev | prod |
|---|---|---|
| 주 DB `ddl-auto` | `update` | `validate` (schema 변경은 수동 migration으로만) |
| SQL 로그(`show_sql`) | 켬 | 끔 |
| Redis host / timeout | `redis` 고정 / 60초 | `${SPRING_DATA_REDIS_HOST:redis}` / 5초 |
| 쿠키 `Secure` | `false` | `true` (profile이 정확히 `prod`일 때만) |

test profile(`application-test.yml`)은 H2 in-memory DB를 사용합니다.

## 5. 구조와 Architecture

```text
Flutter Web (WebUI_FE, :1104) ──HTTP/SSE──► Spring Boot (:4003) ──► MySQL (Constellation_Network, config)
                                                   │
                                                   ├──► Redis (인증 세션, 알림 Pub/Sub)
                                                   └──► Discord OAuth/API
외부 시스템(Discord 봇 등) ──X-Api-Key──► /api/integrations/**
```

패키지 (`AuthServerPlatform/src/main/java/com/help`)

| 패키지 | 책임 |
|---|---|
| `authserver` | Discord OAuth 로그인, 토큰 갱신·로그아웃, 채팅방 선택(`/auth/**`), Redis 세션, `/auth` 전용 JWT 필터 |
| `erpweb.domain.academy` | Academy API와 권한 판정(`AcademyAuthorization`) |
| `erpweb.domain.modules.chatbot` | content·learning·menu·music 저장소 API |
| `erpweb.domain.modules.erp.point` / `.penalty` | 포인트·벌점 API |
| `erpweb.domain.notification` | 알림 발행·조회, `realtime/`의 Redis Pub/Sub 중계와 SSE registry |
| `erpweb.domain.config` / `.metadata` | config DB entity, 동적 metadata 조회 |
| `global` | security·CORS, `/api/**` JWT 필터, 채팅방 context(`guild`, `chat`), 외부 연동 인증(`integration`), 공통 응답·예외, datasource·HTTP client·Clock 설정 |

요청 흐름: `controller`(인증 주체 확인, 입력 검증, DTO 변환) → `service`(업무 규칙, transaction 경계) → `repository`(JPA/native query, stored procedure) → MySQL. 외부 입력은 request DTO에서 Bean Validation으로 검증하고 entity를 그대로 노출하지 않습니다.

Security chain

1. `/api/integrations/**` (`@Order(0)`): `X-Api-Key`만 인정, `ROLE_INTEGRATION`
2. `/auth/**`: 일부 공개(`discord_login`, `check`, `refresh`), 나머지는 access token + Redis 세션 검증, 실패 시 본문 없는 401
3. `/api/**`: 전부 `authenticated()`, 미인증은 401 JSON. `/api/admin/**`은 `ROLE_ADMIN`, 인증된 비관리자는 404(ADR-0002)

관련 문서: [architecture](docs/architecture.md), [ADR-0002 관리자 API 경로](docs/adr/0002-admin-api-prefix.md), [ADR-0003 벌점 이력](docs/adr/0003-penalty-log.md), [ADR-0004 알림](docs/adr/0004-notification.md). ADR-0001(길드 스코프 `botId`)은 코드·migration·다른 ADR에서 참조하지만 저장소에 원문 파일이 없습니다(13장 후속 작업).

## 6. API

- **명세 기준 위치**: Notion `섀버 별자리 Cafe 개발 본부 / 명세서 / API 명세서`의 기능별 페이지입니다. 실제 request/response·권한·오류 계약은 Notion을 기준으로 검토하고, API를 바꾸면 같은 작업에서 해당 페이지를 갱신합니다.

  | Notion 페이지 | 범위 |
  |---|---|
  | WebUI_BE API 공통 규격 | 인증·쿠키, CORS, 공통 응답·오류, 관리자 경로 규칙, 전체 엔드포인트 목록 |
  | Auth API 명세 | `/auth/**` |
  | Academy API 명세 | `/api/academy/**` |
  | ChatBot API 명세 | `/api/repository/{content,learning,menu,music}/**` |
  | Membership API 명세 | `/api/repository/membership/point_log`, `/api/admin/points/**` |
  | Penalty API 명세 | `/api/admin/penalties/**`, `/api/me/penalties` |
  | Notification API 명세 | `/api/me/notifications/**`, `/api/admin/notifications`, `/api/integrations/notifications` |

- 저장소의 [API 개요](docs/api.md)는 탐색용 요약입니다.
- **version 정책**: 경로에 version 접두사(`/v1`)를 두지 않습니다. 외부 client에 공개할 때 다시 결정합니다(ADR-0002). breaking change는 BE·FE를 같은 시점에 `main`으로 병합·배포하고 rollback도 함께 합니다.
- **경로 규칙**: 새 API는 kebab-case·복수형 명사, 동작은 HTTP method로 표현합니다. 관리자 API는 `/api/admin/**`, 본인 데이터는 `/api/me/**`입니다. 기존 `/api/repository/**`, `/api/academy/**`는 아직 이전 규칙을 따릅니다.
- **인증·인가**: `AccessToken`(30초)·`RefreshToken`(1일) HttpOnly 쿠키, Redis 세션 `session:user:{username}`. 채팅방 선택(`POST /auth/guild/select`)을 마친 토큰이어야 `/api/**`를 사용할 수 있습니다.
- **응답·오류**: 공통 `ApiResponse { success, response, error { message, status } }`. 예외로 `GET /api/academy/lesson-records`는 배열을 직접 반환합니다.
- **pagination**: `page`는 1부터, `size`는 1~100(알림 본인 목록은 커서 `beforeId` + `size` 1~50). 범위를 벗어나면 400입니다.
- **idempotency**: 요청 ID로 재전송을 처리하는 endpoint

  | Endpoint | key | 같은 key 재전송 |
  |---|---|---|
  | `POST /api/admin/penalties` | `requestId`(UUID), 길드 단위 | 같은 내용이면 현재 상세 반환, 다른 내용이면 409 |
  | `POST /api/admin/notifications`, `POST /api/integrations/notifications` | `requestId`(영문·숫자·`._:-` 64자 이하) | 같은 내용이면 기존 결과, 다른 내용이면 409 |
  | `PUT /api/me/notifications/read-cursor` | 요청 자체가 멱등 | 읽음 위치는 뒤로 가지 않음 |

  key 보존 기간은 해당 행이 DB에 남아 있는 동안이며, 알림 보존 기간은 미정입니다. 관리자 포인트 입·출금(`POST /api/admin/points/members/{discordId}/transactions`)은 요청 ID가 없어 재전송 시 중복 반영될 수 있습니다(13장 후속 작업).

## 7. Database와 Migration

| datasource | 설정 key | 내용 |
|---|---|---|
| 주 DB (`Constellation_Network`) | `spring.datasource.constellation` | 사용자·Discord 회원·역할, academy, 추천 저장소, 포인트(`CoinTable`, `PayLog`), `PenaltyLog`, `Notification`, `NotificationReadCursor` |
| config DB | `spring.datasource.config_db` | ERP subscriber/module 설정 (`ddl-auto: none`) |

- 모든 instant는 UTC로 저장·교환합니다. DB `DATETIME(3)`에는 애플리케이션이 UTC wall time으로 변환해 넣고, API는 ISO-8601 `Z` 형식으로 반환합니다. 시간 의존 로직은 주입된 `Clock`(`ClockConfig`)을 사용합니다.
- 잔액 변경과 `PayLog` 기록, 벌점·알림 저장은 각각 하나의 transaction으로 처리합니다. 잔액보다 큰 출금은 409입니다.
- Flyway/Liquibase를 쓰지 않습니다. `docs/migrations/`의 SQL을 운영자가 검증·백업 후 **애플리케이션 배포 전에** 수동 적용합니다(expand-only 우선).

| 순서 | 파일 | 내용 | 비고 |
|---|---|---|---|
| 1 | [0001_guild_scope_bot_id.sql](docs/migrations/0001_guild_scope_bot_id.sql) | `bot_id` 스코프 키, `search_sk_by_bot` 함수 | ADR-0001 |
| 2 | [0003_penalty.sql](docs/migrations/0003_penalty.sql) | `PenaltyLog` 생성 | 이미 적용했다면 건너뜀 |
| 3 | [0004_penalty_discord_identity.sql](docs/migrations/0004_penalty_discord_identity.sql) | `PenaltyLog.sk` 제거 | 이후 이전 코드로 rollback하려면 `sk` 컬럼·값을 백업에서 먼저 복원 |
| 4 | [0005_notification.sql](docs/migrations/0005_notification.sql) | `Notification`, `NotificationReadCursor` 추가 | 미적용 시 알림 API와 **관리자 포인트 입·출금이 실패** |

backup 정책과 data 보존 기간은 아직 정하지 않았습니다(9-1장).

## 8. 외부 연동

| 대상 | timeout | retry | 실패 시 동작 |
|---|---|---|---|
| Discord OAuth/API (`RestTemplate`) | connect 3초, read 5초 (`HttpClientConfig`) | 사용자 정보 조회만 429에서 1회, `Retry-After` 1~5초 대기. 그 외 오류는 재시도하지 않음 | 사용자 정보 실패는 503(`EXTERNAL_SERVICE_UNAVAILABLE`), guild 목록 실패는 config DB의 저장 목록으로 대체 |
| Redis | command timeout prod 5초, dev 60초 | 발행 실패는 재시도하지 않음(중복·지연 위험). 구독 끊김은 1초→최대 30초 exponential backoff로 재구독 | 발행 인스턴스의 SSE 연결에만 직접 전달하고 metric 기록. 다른 인스턴스 회원은 재접속·패널 열기 때 DB에서 복구 |
| MySQL | 미정 (드라이버·HikariCP 기본값) | 없음 | 500 |
| SMTP | 미정 (메일 기능 비활성 상태) | 없음 | 해당 없음 |

- 전체 요청 deadline, MySQL·SMTP timeout 값은 확정되지 않았습니다. 운영 배포 전 프로젝트 책임자가 값을 승인하고 구현·테스트·runbook을 함께 갱신해야 합니다.
- 실시간 알림(SSE) 기본값: 연결 유지 30분(`EMITTERTIMEOUTMILLIS`), heartbeat 25초(`HEARTBEATMILLIS`), 회원당 5개(`MAXCONNECTIONSPERUSER`, 초과 시 가장 오래된 연결 종료), 인스턴스당 1000개(`MAXCONNECTIONS`, 초과 시 503). Redis channel은 `webui:notifications`입니다.
- reverse proxy를 두면 `/api/me/notifications/stream`의 응답 buffering을 끄고 read timeout을 heartbeat(25초)보다 길게 설정합니다.
- rate limit: Discord 429 처리 외에 자체 rate limit은 없습니다. 외부 client별 rate limit은 미정입니다(ADR-0004 후속 작업).
- local/test 대체: 테스트는 H2와 mock/fake 경계를 사용하며 실제 Discord·Redis를 호출하지 않습니다.

## 9. 오류 처리와 관측성

- 오류 분류: `ErrorCode` + `GlobalExceptionHandler`. validation·형식 오류 400, 미인증 401, 등록되지 않은 채팅방·채팅방 비회원 403, 인증된 비관리자의 관리자 API 호출과 없는 자원 404(ADR-0002), 요청 ID·잔액 충돌 409, 외부 서비스 장애 503, 그 밖의 예외 500. 외부 응답에 stack trace·내부 구조를 넣지 않습니다.
- health check: liveness `/actuator/health/liveness`, readiness `/actuator/health/readiness`. 상세 정보는 노출하지 않습니다(`show-details: never`).
- metric: `/actuator/prometheus`(외부 요청은 기본 거부, 내부 수집 경로 별도 구성 필요). 알림 metric은 `notification_published_total{source,target}`, `notification_broadcast_failures_total{stage}`, `notification_sse_connections`입니다.
- 관측 대상: 핵심 API의 오류율, p95/p99 latency, 처리량, JVM·connection pool·container saturation.
- graceful shutdown: 종료 단계 제한 30초, Compose `stop_grace_period` 40초. SSE 연결은 먼저 닫아 종료 대기를 늘리지 않습니다.
- masking: 인증 정보, Authorization header, 쿠키·세션 식별자, request/response 원문, 개인정보는 로그에 남기지 않습니다.
- **미정**: 구조화 log·trace의 correlation ID 연결, SLO/성능 목표, alert threshold, alert 담당자, 장애 대응 runbook, dashboard/telemetry query 위치. 운영 배포 전 프로젝트 책임자가 지정해야 합니다.

## 9-1. 문서와 보안 운영 메타데이터

| 항목 | 현재 상태 |
|---|---|
| 문서 담당자 / 최종 검증일 | 미정 / 2026-09-28 |
| 관련 문서 | `docs/adr/`, `docs/api.md`, `docs/deploy.md`, Notion API 명세서 |
| 민감 정보 분류 | 비밀값(DB·JWT·Discord secret, 외부 API Key), 인증 쿠키·Redis 세션의 Discord access token, Discord ID·닉네임 |
| 로그 보존 기간 / 접근 권한 | 미정 |
| secret 저장 / rotation 책임 | GitHub Actions secrets로 주입 / 담당자·주기 미정 |
| 외부 API Key | 서버에는 SHA-256만 보관. 발급·전달·회전 담당자는 첫 client 등록 전에 지정 필요 |
| 보안 incident 대응 | runbook 미정. 노출이 의심되면 삭제에 그치지 않고 즉시 폐기·회전하고, 외부 발행 이력(`source=EXTERNAL`, `source_ref`) 등으로 영향 범위를 조사 |

## 10. 테스트

- **Unit**: JWT, 인증 세션, Discord API·로그인 서비스, 벌점 entity·service, 포인트 service, 알림 command·service, SSE registry·Redis 중계, Academy 권한·service
- **Controller/MVC**: 입력 검증(ChatBot content·learning, 벌점), 관리자 경로·권한(포인트 route, 벌점 authorization, `JsonAccessDeniedHandler`), 알림 controller, 외부 API Key 필터
- **Integration**: `LearningRepositoryTest`, `PenaltyRepositoryIntegrationTest` (H2)
- **Contract**: Notion 명세와의 자동 contract test는 아직 없고 review로 확인합니다.
- **보강 필요**: pagination 최대 크기 전 endpoint, timeout/retry/fallback, 관리자 포인트 중복 요청, transaction rollback, Academy·ChatBot 소유권 검증
- test data에는 실제 개인정보·비밀값을 쓰지 않습니다. 벌점·알림 service 테스트는 고정 `Clock`을 주입해 시간에 의존하지 않습니다.

CI gate(`.github/workflows/CI.yml`, PR → `main`/`develope`, push → `develope`)

1. Gradle wrapper 검증
2. `./gradlew clean check bootJar --no-daemon` (Spotless·EditorConfig·Checkstyle·테스트·jar)
3. Docker image build

gate가 실패하면 merge하지 않습니다. 우회가 필요하면 사유·위험·기간·후속 조치를 PR에 기록하고 승인받습니다.

## 11. Build·배포·운영

- artifact: `Dockerfile` multi-stage(`gradlew clean bootJar` → `eclipse-temurin:17-jre-jammy`, non-root, 포트 4003)
- 배포(`.github/workflows/CD.yml`): `main` push → SCP로 원격 서버에 파일 전송 → `docker compose -p webui-be config --quiet` → `up -d --build --remove-orphans`. 동시 배포는 막혀 있습니다(`concurrency: webui-be-production`).
- 배포 순서: ① DB backup ② 필요한 migration 수동 적용 ③ BE `main` 병합·배포 ④ readiness 확인 ⑤ API 계약이 바뀐 경우 FE를 같은 시점에 배포
- rollback: 이전 검증 완료 commit으로 roll-forward하거나 image/reference를 되돌린 뒤 readiness를 확인합니다. schema를 줄이는 migration(0004 등) 이후에는 백업 복원이 선행되어야 합니다.
- 상세 절차: [deploy 문서](docs/deploy.md)

## 12. 주요 Dependency

| Dependency | 용도 |
|---|---|
| Spring Boot 3.4.3 starters (web, webflux, data-jpa, security, validation, data-redis, mail, actuator) | API, 보안, persistence, Redis, health |
| `io.jsonwebtoken:jjwt-*` 0.13.0 | JWT 발급·검증 |
| `micrometer-registry-prometheus` | metric 노출 |
| `mysql-connector-j` / `h2`(test) | DB driver |
| Lombok | boilerplate 제거 |
| Checkstyle(Naver rules), Spotless 8.10.2, EditorConfig 0.0.3 | 정적 검사·formatting |

- Dependabot이 Gradle, GitHub Actions, Docker dependency 업데이트 PR을 매주(각 최대 5개) 제안합니다.
- Gradle lockfile은 사용하지 않고 plugin·library version을 `build.gradle`과 Spring dependency management로 고정합니다. 추가·업데이트 시 필요성, 유지보수 상태, license, 취약점, runtime 영향을 검토합니다.

## 13. License와 알려진 후속 작업

- 프로젝트 license: 미정 (지정 필요)
- 외부 code·asset license 고지: 해당 없음 (현재 외부 asset 미포함)

알려진 문서·구현 불일치 (2026-09-28 기준)

- ADR-0001(길드 스코프) 원문이 `docs/adr/`에 없습니다.
- `docs/api.md`에 Academy, `/auth/guilds`, `/auth/guild/select` endpoint가 빠져 있습니다. 전체 목록은 Notion 공통 규격을 봅니다.
- `docs/deploy.md`의 `.env` 예시에 `DB_CONFIG_URL`, `DISCORD_GUILDS_URI`가 빠져 있습니다. 필수 변수는 4장을 따릅니다.
- `docs/architecture.md`는 Redis를 세션 저장소로만 표기합니다(알림 Pub/Sub 미반영).
- `KisProperties`/`WebClientConfig`(한국투자증권 mock API) bean은 있으나 endpoint에서 사용하지 않습니다.
- 보안 review 대상: ChatBot 저장·삭제의 소유권 검증, lesson-record 생성의 academy/class 권한 범위, OAuth `state` 미검증(상세는 Notion 공통 규격 9장).
