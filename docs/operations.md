# 운영: 외부 연동·관측성·보안

> 상태: Active  
> 마지막 검토일: 2026-09-28  
> 상위 문서: [README](../README.md) · 관련: [설정](configuration.md), [deploy](deploy.md), [ADR-0004](adr/0004-notification.md)

값이 정해지지 않은 항목은 임의로 채우지 않고 "미정"과 결정 책임을 적습니다. 운영 배포 전 프로젝트 책임자가 지정해야 합니다.

## 외부 연동과 복원력

| 대상 | timeout | retry | 실패 시 동작 |
|---|---|---|---|
| Discord OAuth/API (`RestTemplate`) | connect 3초, read 5초 (`HttpClientConfig`) | 사용자 정보 조회만 429에서 1회, `Retry-After` 1~5초 대기. 그 외 오류는 재시도하지 않음(side effect·요청 thread 점유 위험) | 사용자 정보 실패는 503(`EXTERNAL_SERVICE_UNAVAILABLE`), guild 목록 실패는 config DB에 저장된 목록으로 대체 |
| Redis 명령 | prod 5초, dev 60초 | 알림 발행 실패는 재시도하지 않음(중복·지연 위험) | 발행 인스턴스의 SSE 연결에만 직접 전달하고 metric 기록. 다른 인스턴스 회원은 재접속·패널 열기 때 DB에서 복구 |
| Redis 구독 | — | 1초 → 최대 30초 exponential backoff로 재구독 | 재구독 전까지 다른 인스턴스 발행분 실시간 전달 누락 |
| MySQL | 미정 (드라이버·HikariCP 기본값) | 없음 | 500 |
| SMTP | 해당 없음 (메일 기능 비활성) | — | — |

- 전체 요청 deadline과 MySQL timeout 값은 미정입니다. 정하면 구현·자동 테스트·이 문서를 함께 갱신합니다.
- rate limit: Discord 429 처리 외 자체 제한은 없습니다. 외부 client별 rate limit은 미정입니다(ADR-0004 후속 작업).
- 테스트는 H2와 mock/fake 경계를 쓰며 실제 Discord·Redis를 호출하지 않습니다.

### 실시간 알림(SSE)

- 기본값: 연결 유지 30분, heartbeat 25초, 회원당 5개(초과 시 가장 오래된 연결 종료), 인스턴스당 1000개(초과 시 503). 조정 변수는 [설정](configuration.md#1-환경-변수).
- reverse proxy를 두면 `/api/me/notifications/stream`의 응답 buffering을 끄고 read timeout을 heartbeat(25초)보다 길게 설정합니다.
- graceful shutdown 때 SSE 연결을 먼저 닫아 종료 대기를 늘리지 않습니다.

## 관측성

| 항목 | 현재 |
|---|---|
| liveness | `/actuator/health/liveness` |
| readiness | `/actuator/health/readiness` (상세 비노출) |
| metric | `/actuator/prometheus` — 외부 요청은 기본 거부, 내부 수집 경로 별도 구성 필요 |
| 알림 metric | `notification_published_total{source,target}`, `notification_broadcast_failures_total{stage}`, `notification_sse_connections` |
| 관측 대상 | 핵심 API 오류율, p95/p99 latency, 처리량, JVM·connection pool·container saturation |
| correlation ID (log·metric·trace 연결) | 미정 |
| SLO / 성능 목표 | 미정 |
| alert threshold / 담당자 | 미정 |
| dashboard / telemetry query 위치 | 미정 |
| 장애 대응 runbook | 미정 |

graceful shutdown: 종료 단계 제한 30초, Compose `stop_grace_period` 40초.

## 로그

- 인증 정보, Authorization header, 쿠키·세션 식별자, request/response 원문, 개인정보는 기록하지 않습니다.
- 로그 보존 기간과 접근 권한: 미정

## 보안 운영

| 항목 | 현재 |
|---|---|
| 민감 정보 분류 | 비밀값(DB·JWT·Discord secret, 외부 API Key), 인증 쿠키, Redis 세션의 Discord access token, Discord ID·닉네임 |
| secret 저장 | GitHub Actions secrets → runtime 환경 변수 |
| secret rotation 주기·담당자 | 미정 |
| 외부 API Key | 서버에는 SHA-256만 보관. 발급·전달·회전 담당자는 첫 client 등록 전에 지정 필요 |
| 접근 권한 승인·회수·감사 | 미정 |
| incident 대응 | runbook 미정. 노출이 의심되면 즉시 폐기·회전하고, 외부 발행 이력(`source=EXTERNAL`, `source_ref`) 등으로 영향 범위를 조사 |

알려진 보안 review 대상(상세: Notion `WebUI_BE API 공통 규격` 9장)

- ChatBot 저장·삭제 API가 요청한 `recommender`/`teacher`가 본인인지 확인하지 않음
- lesson-record 생성 시 body의 academy/class 권한 범위를 확인하지 않음
- OAuth `state` 미검증, 쿠키 인증인데 CSRF 비활성
- 서비스 내부 `IllegalArgumentException`·`AccessDeniedException`이 500으로 나갈 수 있음
