# 빗자루 WebUI Backend

> 상태: Active  
> 적용 범위: `ConstellationCafe/WebUI_BE` 백엔드 서비스 (`develope` 기준)  
> 문서 담당자: 미정 — 프로젝트 책임자가 지정 필요  
> 마지막 검토일: 2026-09-30

## 1. 프로젝트 개요

[빗자루](https://github.com/ConstellationCafe/ModularDiscordBot)는 섀버 별자리 Cafe에서 운영하는 Discord 채팅 봇입니다. 이 저장소는 명령어 기반 봇 조작의 한계를 보완하는 WebUI([WebUI_FE](https://github.com/ConstellationCafe/WebUI_FE))가 호출하는 Spring Boot API 서버입니다.

주요 기능

- **인증**: Discord OAuth 2.0 로그인, JWT(HttpOnly 쿠키) + Redis 세션, 채팅방(`botId`) 선택 후 방 단위 권한 적용([ADR-0001](docs/adr/0001-guild-scope.md))
- **메뉴 설정**: 현재 JWT의 `botId`로 ModuleConfig를 조회해 모듈 ID와 아카데미·대회 활성 여부만 반환. FE는 활성 기능에 한해서 서버 권한을 조회([ADR-0005](docs/adr/0005-module-config-menu.md))
- **ChatBot 저장소**: 콘텐츠·학습 자료·메뉴·음악 추천 데이터 조회와 일괄 저장·삭제
- **Academy**: 아카데미·반·과목, 학생·강사 현황, 수업 기록 작성·조회·수정·삭제
- **ERP 포인트**: 본인 포인트 내역, 관리자 포인트 입·출금과 내역 수정·삭제
- **ERP 벌점**: 관리자 벌점 부여·취소·이력·30일 누적 순위, 본인 벌점 조회([ADR-0003](docs/adr/0003-penalty-log.md))
- **알림**: 채팅방 전체/회원 대상 알림 발행(관리자·내부 기능·외부 시스템), SSE 실시간 전달([ADR-0004](docs/adr/0004-notification.md))

기술 stack: Java 17, Spring Boot 3.4.3(Web, WebFlux, Data JPA, Security, Validation, Data Redis, Mail, Actuator), MySQL, Redis, JJWT 0.13.0, Micrometer Prometheus

## 2. 문서 목록

| 문서 | 내용 |
|---|---|
| [docs/architecture.md](docs/architecture.md) | 시스템 구성, 패키지 책임, 요청 흐름, 인증·채팅방 스코프, security chain |
| [docs/api.md](docs/api.md) | API 공통 규칙, 전체 endpoint, idempotency. 계약 기준은 Notion 명세 |
| [docs/configuration.md](docs/configuration.md) | 환경 변수, 주입 방식, profile별 차이 |
| [docs/deploy.md](docs/deploy.md) | 로컬·Compose 실행, CI/CD, 배포 순서, rollback |
| [docs/migrations/README.md](docs/migrations/README.md) | 수동 DB migration 원칙·목록·적용 순서 |
| [docs/operations.md](docs/operations.md) | 외부 연동 timeout·retry, 관측성, 로그, 보안 운영(미정 항목 포함) |
| [docs/adr/README.md](docs/adr/README.md) | ADR 목록(0001~0005) |

## 3. 개발 환경

| 항목 | 요구사항 | 근거 |
|---|---|---|
| JDK | 17 (Temurin 권장) | `build.gradle` toolchain, `Dockerfile`, CI |
| Gradle | 9.7.1 Wrapper (`AuthServerPlatform/gradlew`) | `gradle-wrapper.properties` |
| MySQL | 8 이상, datasource 2개(주 DB, config DB) | `application-*.yml` |
| Redis | 7.4 (Compose는 `redis:7.4.2-alpine`) | `redis/Dockerfile` |
| Docker / Docker Compose | 컨테이너 실행·배포 시 | `Dockerfile`, `docker-compose.yml` |

지원 운영체제: JDK 17과 Docker가 동작하는 Linux·macOS·Windows. 운영 환경은 Linux 컨테이너(`eclipse-temurin:17-jre-jammy`, non-root)입니다.

## 4. 빠른 시작과 검증

```bash
cd AuthServerPlatform

# 실행 (필수 환경 변수는 docs/configuration.md)
./gradlew bootRun --args='--spring.profiles.active=dev'

# 전체 검증: Spotless·EditorConfig·Checkstyle·테스트·배포 jar
./gradlew clean check bootJar --no-daemon
```

성공 기준

- `check bootJar`가 `BUILD SUCCESSFUL`로 끝나고 `build/libs/*.jar`가 생성됩니다.
- 실행 후 `GET http://localhost:4003/actuator/health/readiness`가 `UP`을 반환합니다.

Docker Compose 실행, `.env` 예시, CI/CD와 배포 순서는 [deploy](docs/deploy.md)를 봅니다.

## 5. 설정 요약

필수 환경 변수: `SPRING_PROFILES_ACTIVE`, `DB_URL`, `DB_CONFIG_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `DISCORD_CLIENT_ID`, `DISCORD_CLIENT_SECRET`, `DISCORD_REDIRECT_URI`, `DISCORD_TOKEN_URI`, `DISCORD_USER_URI`, `DISCORD_GUILDS_URI`, `FRONT_REDIRECT_URI`, `REGISTER_URI`

- 비밀값은 커밋하지 않습니다. local은 셸·IDE 또는 커밋되지 않는 `.env`, production은 GitHub Actions secrets로 주입합니다.
- 외부 알림 발행 client(`INTEGRATION_CLIENTS_*`)와 실시간 알림 조정값(`NOTIFICATION_REALTIME_*`)은 선택입니다.
- dev는 주 DB `ddl-auto: update`, prod는 `validate`입니다.

변수별 의미·예시 형식과 profile 차이: [configuration](docs/configuration.md)

## 6. 구조 요약

```text
Controller(검증·변환·인가) → Service(업무 규칙·transaction) → Repository → MySQL
                                        └── Discord API, Redis
```

- `authserver`: 로그인·세션·채팅방 선택(`/auth/**`)
- `erpweb.domain`: academy, chatbot 저장소, erp(point·penalty), notification
- `global`: security, JWT, 채팅방 context(`GuildContext`), 외부 연동 인증, 공통 응답·예외, 설정

상세: [architecture](docs/architecture.md)

## 7. API

- **명세 기준 위치**: Notion `섀버 별자리 Cafe 개발 본부 / 명세서 / API 명세서`의 기능별 페이지. API를 바꾸면 같은 작업에서 해당 페이지와 [API 개요](docs/api.md)를 갱신합니다.
- 인증: `AccessToken`(30초)·`RefreshToken`(1일) HttpOnly 쿠키 + Redis 세션. `/api/**`는 채팅방 선택을 마친 토큰이 필요합니다.
- 관리자 API는 `/api/admin/**`(`ROLE_ADMIN`, 비관리자 404), 본인 데이터는 `/api/me/**`([ADR-0002](docs/adr/0002-admin-api-prefix.md)). version 접두사는 두지 않습니다.
- 응답은 공통 `ApiResponse { success, response, error }`, pagination은 `page` 1부터·`size` 1~100입니다.
- 벌점 부여·알림 발행은 `requestId`로 재전송을 처리합니다. 관리자 포인트 입·출금은 요청 ID가 없습니다.

## 8. Database와 Migration

- 주 DB(`Constellation_Network`)와 config DB 두 datasource를 사용하고, 같은 DB를 빗자루 봇도 사용합니다.
- 모든 instant는 UTC로 저장·교환하고, 시간 의존 로직은 주입된 `Clock`을 씁니다.
- Flyway/Liquibase 없이 `docs/migrations/`의 SQL을 **애플리케이션 배포 전에** 수동 적용합니다. 현재 순서: 0001 → 0003 → 0004 → 0005. 0005를 적용하지 않으면 알림 API와 관리자 포인트 입·출금이 실패합니다.

원칙·절차·rollback: [migrations](docs/migrations/README.md)

## 9. 외부 연동·관측성·보안 요약

- Discord: connect 3초 / read 5초, 사용자 정보 조회만 429에서 1회 재시도. Redis 명령 timeout prod 5초.
- health: `/actuator/health/liveness`, `/actuator/health/readiness`. metric: `/actuator/prometheus`(내부 수집 경로 별도 필요).
- graceful shutdown 30초, Compose stop 유예 40초.
- 인증 정보·세션 식별자·request/response 원문·개인정보는 로그에 남기지 않습니다.
- SLO, alert, runbook, 로그 보존 기간, secret rotation 담당자 등은 **미정**입니다.

상세 표와 미정 항목: [operations](docs/operations.md)

## 10. 테스트

- **Unit**: JWT, 인증 세션, Discord API·로그인, 벌점 entity·service, 포인트 service, 알림 command·service, SSE registry·Redis 중계, Academy 권한·service
- **Controller/MVC**: 입력 검증(ChatBot, 벌점), 관리자 경로·권한(포인트, 벌점, `JsonAccessDeniedHandler`), 알림 controller, 외부 API Key 필터
- **Integration**: `LearningRepositoryTest`, `PenaltyRepositoryIntegrationTest` (H2)
- **Contract**: Notion 명세와의 자동 contract test는 아직 없고 review로 확인합니다.
- **보강 필요**: pagination 최대 크기 전 endpoint, timeout/retry/fallback, 관리자 포인트 중복 요청, transaction rollback, Academy·ChatBot 소유권 검증
- test data에는 실제 개인정보·비밀값을 쓰지 않습니다. 벌점·알림 service 테스트는 고정 `Clock`을 주입합니다.

CI gate: PR(→ `main`/`develope`)과 `develope` push에서 Gradle wrapper 검증, `./gradlew clean check bootJar --no-daemon`, Docker image build가 통과해야 merge합니다. 우회가 필요하면 사유·위험·기간·후속 조치를 PR에 기록하고 승인받습니다.

## 11. Build·배포

- artifact: `Dockerfile` multi-stage(`gradlew clean bootJar` → JRE 17, 포트 4003)
- 배포: `main` push → CD가 원격 서버에서 `docker compose up -d --build --remove-orphans`
- 순서: DB backup → migration → BE 배포 → readiness 확인 → (계약 변경 시) FE 동시 배포
- rollback: 이전 검증 완료 commit으로 되돌려 재배포. 경로 변경 릴리스는 FE와 함께 되돌립니다.

상세: [deploy](docs/deploy.md)

## 12. 주요 Dependency

| Dependency | 용도 |
|---|---|
| Spring Boot 3.4.3 starters | API, 보안, persistence, Redis, health |
| `io.jsonwebtoken:jjwt-*` 0.13.0 | JWT 발급·검증 |
| `micrometer-registry-prometheus` | metric 노출 |
| `mysql-connector-j` / `h2`(test) | DB driver |
| Lombok | boilerplate 제거 |
| Checkstyle(Naver rules), Spotless 8.10.2, EditorConfig 0.0.3 | 정적 검사·formatting |

- Dependabot이 Gradle, GitHub Actions, Docker dependency 업데이트 PR을 매주 제안합니다.
- Gradle lockfile은 쓰지 않고 `build.gradle`과 Spring dependency management로 version을 고정합니다. 추가·업데이트 시 필요성, 유지보수 상태, license, 취약점, runtime 영향을 검토합니다.

## 13. License와 후속 작업

- 프로젝트 license: 미정 (지정 필요)
- 외부 code·asset license 고지: 해당 없음

알려진 후속 작업

- 관리자 포인트 입·출금 idempotency(요청 ID) 도입
- `docker-compose.yml`·CD에 `INTEGRATION_CLIENTS_*` 전달 추가(외부 발행을 켤 때)
- `KisProperties`/`WebClientConfig`(한국투자증권 mock API) 미사용 bean 정리
- 보안 review 대상과 미정 운영 항목: [operations](docs/operations.md)
