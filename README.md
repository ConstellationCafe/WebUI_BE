# 빗자루 WebUI Backend

> 상태: Active  
> 적용 범위: `ConstellationCafe/WebUI_BE` 백엔드 서비스  
> 문서 담당자·마지막 운영 검증일: 프로젝트에서 지정 필요

[빗자루](https://github.com/ConstellationCafe/DiscordBot)의 WebUI가 사용하는 Spring Boot API 서버입니다. Discord OAuth 2.0 인증, JWT 기반 인증 상태, 콘텐츠·학습 자료·메뉴·음악 추천 및 아카데미 관리 기능을 제공합니다.

## 기술 스택과 요구사항

- Java 17
- Gradle 8.13 Wrapper
- Spring Boot 3.4.3
- MySQL 8+
- Redis 7.4+
- Docker 및 Docker Compose(컨테이너 실행 시)

## 빠른 시작

저장소의 비밀값은 커밋하지 않습니다. 아래 환경 변수는 로컬 셸, 승인된 secret manager 또는 배포 시스템에서 주입합니다.

```bash
cd AuthServerPlatform
./gradlew bootRun --args='--spring.profiles.active=dev'
```

Docker Compose로 실행할 때는 저장소에 커밋되지 않는 `.env`에 값을 주입한 뒤 실행합니다.

```bash
docker compose config
docker compose up -d --build
docker compose ps
```

## 검증

```bash
cd AuthServerPlatform
./gradlew clean check bootJar --no-daemon
```

`check`는 unit/integration test와 Checkstyle을 실행하고, `bootJar`는 배포 artifact 생성을 검증합니다. Pull Request와 `develope`, `codex/**` push에서는 같은 검증과 Docker image build가 CI에서 수행됩니다.

## 설정

| 변수 | 필수 | 용도 | 안전한 예시 형식 |
|---|---:|---|---|
| `SPRING_PROFILES_ACTIVE` | 예 | 실행 profile | `dev` 또는 `prod` |
| `DB_URL` | 예 | 주 데이터베이스 JDBC URL | `jdbc:mysql://db:3306/database` |
| `DB_CONFIG_URL` | 예 | 설정 데이터베이스 JDBC URL | `jdbc:mysql://db:3306/config` |
| `DB_USER` | 예 | 데이터베이스 사용자 | `service_user` |
| `DB_PASSWORD` | 예 | 데이터베이스 비밀값 | 값은 secret manager에서 주입 |
| `JWT_SECRET` | 예 | JWT 서명 비밀값 | 충분한 길이의 무작위 값 |
| `DISCORD_CLIENT_ID` | 예 | Discord OAuth client ID | Discord 발급 값 |
| `DISCORD_CLIENT_SECRET` | 예 | Discord OAuth secret | 값은 secret manager에서 주입 |
| `DISCORD_REDIRECT_URI` | 예 | OAuth callback URI | `https://example.test/callback` |
| `DISCORD_TOKEN_URI` | 예 | Discord token endpoint | HTTPS URI |
| `DISCORD_USER_URI` | 예 | Discord user endpoint | HTTPS URI |
| `DISCORD_GUILDS_URI` | 예 | Discord guilds endpoint | HTTPS URI |
| `DISCORD_GUILD_INFO_URI` | 예 | Discord guild detail endpoint | HTTPS URI template |
| `FRONT_REDIRECT_URI` | 예 | 로그인 후 frontend URI | HTTPS URI |
| `REGISTER_URI` | 예 | 가입 안내 URI | HTTPS URI |
| `SPRING_DATA_REDIS_HOST` | 아니요 | Redis host | 기본값 `redis` |

Development는 편의를 위해 primary schema를 `update`할 수 있지만 production은 `validate`만 수행합니다. 운영 schema 변경은 별도 검토·백업·복구 계획을 가진 migration으로 수행해야 합니다.

## 구조와 요청 흐름

```text
HTTP Controller -> request validation/DTO -> service/domain -> repository -> MySQL
                                      \-> Discord/Redis 등 외부 경계
```

- `controller`: 인증 주체 확인, 입력 검증, transport 변환
- `service`: 업무 규칙과 transaction 경계
- `repository`: persistence 및 query
- `dto`: 외부 계약용 request/response 모델
- `config`/`global`: security, error handling, 공통 infrastructure

세부 구조는 [architecture 문서](docs/architecture.md)를 참고합니다.

## API와 보안

- 프로젝트 API 명세의 기준 위치는 Notion의 `/명세서/API 명세서` 아래 기능별 페이지입니다.
- 저장소의 [API 개요](docs/api.md)는 탐색용 요약이며 실제 request/response·권한·오류 계약은 Notion 명세를 기준으로 검토합니다.
- 인증 정보, Authorization header, session identifier, request/response 원문, 개인정보는 로그에 기록하지 않습니다.
- 비밀값은 GitHub Actions secrets 또는 승인된 runtime secret 관리 수단으로 주입합니다.
- 노출이 의심되면 값을 삭제하는 데 그치지 않고 즉시 폐기·회전하고 이력과 영향을 조사합니다.

## 외부 연동과 복원력

Discord, MySQL, Redis, SMTP가 외부 경계입니다. Discord HTTP 연결 timeout은 3초, 응답 timeout은 5초이며, Redis command timeout은 production에서 5초입니다. Discord 요청은 side effect와 요청 thread 점유 위험 때문에 자동 재시도하지 않습니다. 사용자 정보 조회 실패는 503 API 오류로 변환하고, guild 조회 실패 시에는 저장된 guild 목록으로 대체합니다. SMTP·DB의 connection/read/overall timeout과 전체 요청 deadline은 아직 확정되지 않았으므로 운영 배포 전 프로젝트 책임자가 값을 승인하고 구현·자동 테스트·runbook을 함께 갱신해야 합니다.

## 관측성과 운영

- liveness: `/actuator/health/liveness`
- readiness: `/actuator/health/readiness`
- Prometheus metrics: `/actuator/prometheus` (외부 요청은 기본 거부하며 내부 수집 경로를 별도로 구성해야 함)
- health 상세 정보는 외부 응답에 노출하지 않습니다.
- 애플리케이션은 graceful shutdown을 사용하며 종료 단계 제한은 30초, Compose stop 유예는 40초입니다.

오류율, p95/p99 latency, 처리량, JVM·connection pool·container saturation을 관측해야 합니다. SLO, alert threshold, alert 담당자, log 보존 기간·접근 권한, dashboard/telemetry query, secret rotation 책임, incident runbook은 현재 미정이며 운영 배포 전 프로젝트 책임자가 지정해야 합니다.

## 테스트 범위

- Unit: 핵심 domain 규칙과 입력 경계
- Integration: security, serialization, persistence, transaction rollback
- Contract: Notion 명세와 endpoint 계약
- 운영 전 보강 필요: 권한 실패, pagination 최대 크기, timeout/retry/fallback, 중복 요청, 시간대/UTC 처리

테스트 데이터에는 실제 개인정보나 비밀값을 사용하지 않습니다.

## Build·배포·복구

CI가 `check`, `bootJar`, Docker build를 통과한 뒤에만 배포합니다. `main` push는 CD가 원격 서버에 파일을 전송하고 `docker compose up -d --build --remove-orphans`를 수행합니다. 배포 전 DB migration 호환성과 backup을 확인합니다. 실패 시에는 이전 검증 완료 commit으로 roll-forward 또는 image/reference를 되돌리고 readiness를 확인합니다.

## Dependency와 라이선스

Dependabot이 Gradle, GitHub Actions, Docker dependency 업데이트 PR을 매주 제안합니다. 추가·업데이트 시 필요성, 유지보수 상태, 라이선스, 취약점과 runtime 영향을 검토합니다. 프로젝트 라이선스와 외부 코드·asset 고지는 현재 지정이 필요합니다.
