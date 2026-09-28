# ADR-0004: 알림 저장·실시간 전달 구조 (DB inbox + Redis Pub/Sub + SSE)

- 상태: Accepted
- 날짜: 2026-09-28
- 적용 범위: WebUI_BE, WebUI_FE
- 담당자: 미정 (프로젝트 문서 담당자 지정 필요)
- 관련: ADR-0001(길드 스코프), ADR-0002(관리자 API 경로), `docs/migrations/0005_notification.sql`

## Context

- 관리자가 알림을 발행하면 채팅방 회원이 WebUI 우측 상단 종 아이콘으로 받아 보고, 새 알림이 있으면 빨간 점으로 표시해야 한다.
- 발행 경로는 세 가지다: 관리자 화면, WebUI_BE 내부 기능(함수 호출), WebUI 밖의 시스템(HTTP API).
- 사용자가 오프라인일 때 발행된 알림도 다음 접속 때 볼 수 있어야 한다.
- 브라우저(Flutter Web)는 Kafka 같은 브로커 프로토콜을 직접 쓸 수 없고, 쓸 수 있다 해도 발행 권한이 있는 인증 정보를 client bundle에 넣게 되어 보안 가이드에 어긋난다.
- 현재 BE는 단일 인스턴스이고 Redis(세션 저장소)를 이미 운영한다.

## Decision

1. **DB가 원본(inbox)이다.** 모든 발행은 `NotificationService.publish`로 모여 먼저 `Notification` 테이블에 저장한다. 오프라인 사용자는 접속 시 REST(`/api/me/notifications`, `/unread-count`)로 목록과 읽지 않은 개수를 다시 맞춘다.
2. **실시간 전달은 at-most-once다.** 커밋 이후(`@TransactionalEventListener(AFTER_COMMIT)`) Redis Pub/Sub channel(`webui:notifications`)로 흘리고, 각 인스턴스가 자기에게 연결된 회원에게 SSE(`/api/me/notifications/stream`)로 보낸다. 놓친 알림은 1번으로 복구되므로 메시지 보관이 필요 없다.
3. **SSE를 쓴다.** 서버→브라우저 단방향이면 충분하고, 기존 HttpOnly `AccessToken` 쿠키가 그대로 실리므로 토큰을 URL에 넣지 않는다. 연결될 때마다 첫 이벤트 `ready`로 읽지 않은 요약을 보내 클라이언트가 상태를 다시 맞춘다. 브라우저 자동 재연결 외에 인증 만료(401) 시 클라이언트가 토큰 갱신 후 다시 연결한다.
4. **읽음은 회원별 커서 하나로 관리한다.** `NotificationReadCursor(bot_id, discord_id, last_read_id)`. 채팅방 전체 알림을 회원 수만큼 복제하지 않고, `id > last_read_id`인 알림이 읽지 않은 알림이다. 알림 패널을 열어 최신 알림을 보면 커서를 그 ID로 옮기고(빨간 점 제거), 커서는 뒤로 가지 않는다(`GREATEST`).
5. **발행 경로별 인증과 멱등성**
   - 관리자: `POST /api/admin/notifications`(ADR-0002 규칙, `ROLE_ADMIN`). 대상 채팅방은 로그인한 채팅방으로 고정.
   - 외부: `POST /api/integrations/notifications`. 별도 SecurityFilterChain에서 `X-Api-Key`를 SHA-256으로 비교하고, client별 허용 botId 목록 안에서만 발행한다. 키 원문은 저장하지 않고 해시만 설정으로 주입한다. client를 설정하지 않으면 기능은 꺼져 있다(모두 401).
   - 내부: `NotificationPublisher.publish(NotificationCommand.internal(...))`. 호출자 트랜잭션에 참여하므로 호출자 작업과 알림 저장이 함께 커밋·롤백된다. 첫 사용처로 관리자 포인트 입·출금 시 대상 회원에게 개인 알림을 보낸다.
   - 관리자·외부 요청은 `requestId`가 필수이며 `(bot_id, source, source_ref, request_key)` 유니크 키로 재전송을 한 건으로 묶는다. 같은 ID·같은 내용이면 처음 결과를 돌려주고(`created=false`, 재전달 없음), 다른 내용이면 409. 저장은 `INSERT ... ON DUPLICATE KEY UPDATE id = id`로 해서 트랜잭션이 rollback-only가 되지 않게 한다(ADR-0003과 같은 방식).
6. **전달 계층은 인터페이스로 분리한다.** `NotificationBroadcaster` 구현만 바꾸면 Kafka 등으로 옮길 수 있고, 발행 API·DB·FE 계약은 바뀌지 않는다.

## 검토한 대안

- **Kafka**: topic 보관과 다수 소비자(봇, 이메일, 모바일 push)가 필요할 때 적합하다. 지금은 소비자가 WebUI 하나이고 외부 발행은 HTTP API로 충분하며, 브로커 운영·메모리·모니터링 부담이 더해진다. 브라우저는 어차피 BE를 거쳐야 하므로 오프라인 문제도 Kafka가 아니라 DB inbox가 해결한다. 다수 소비자가 생기면 6번 경계에서 다시 검토한다.
- **Redis Stream / Kafka로 오프라인 보관**: 보관은 DB가 이미 하고 있어 중복이다.
- **WebSocket**: 양방향이 필요 없고 proxy·인증 처리가 SSE보다 복잡하다.
- **Polling만 사용**: 구현은 가장 단순하지만 즉시성이 떨어지고 요청 수가 접속자 수에 비례해 늘어난다. FE는 SSE 연결 실패 시에만 REST 재조회로 보완한다.
- **알림별 읽음 행(fan-out on write)**: 개별 읽음 표시가 가능하지만 채팅방 전체 알림마다 회원 수만큼 행이 생긴다. 요구사항(새 알림 표시와 읽으면 제거)은 커서로 충족된다.

## Consequences

- 실시간 전달이 Redis 장애로 실패하면 발행 인스턴스의 로컬 연결에만 전달되고 `notification.broadcast.failures{stage=publish}` metric이 증가한다. 다른 인스턴스의 회원은 재접속·패널 열기 시 복구된다. 자동 재시도는 하지 않는다(중복·요청 지연 위험).
- Redis 발행은 요청 thread에서 커밋 직후 동기로 수행되며 Redis command timeout(운영 5초)만큼 응답이 늦어질 수 있다. 부하가 문제되면 비동기 실행기로 분리한다.
- 동일 `requestId`의 동시 요청이 같은 밀리초에 경합하면 드물게 같은 알림이 두 번 전달될 수 있다. 알림 ID는 하나이므로 FE가 ID로 중복을 제거한다.
- 인스턴스당 SSE 연결 수(기본 1000)와 회원당 연결 수(기본 5)를 제한한다. 초과 시 503, 회원당 초과 시 가장 오래된 연결을 닫는다. 연결은 30분마다 끊고 다시 연결하며 25초마다 heartbeat를 보낸다.
- `/api/**` 체인에서 비동기 dispatch(`DispatcherType.ASYNC`)는 다시 인가하지 않는다. 최초 요청이 이미 인가된 SSE 응답의 후속 쓰기이기 때문이다.
- 신규 테이블 두 개가 필요하므로 배포 전 0005 migration을 수동 적용해야 한다. 포인트 입·출금도 알림 테이블에 쓰므로 migration 없이 배포하면 입·출금이 실패한다.
- 알림 보존 기간, 외부 client별 rate limit은 아직 정하지 않았다.

## 후속 작업

- [ ] 알림 보존 기간과 정리 작업 결정 (결정 책임: 프로젝트 책임자)
- [ ] 외부 client 발급·회전 절차 runbook 작성, 첫 client(Discord 봇) 등록
- [ ] 외부 API rate limit 필요성 검토
- [ ] 다중 인스턴스 배포 시 Redis 장애 알림 threshold 설정
