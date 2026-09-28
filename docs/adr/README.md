# ADR 목록

> 상위 문서: [README](../../README.md)

장기간 영향을 주는 architecture, data model, protocol, 주요 dependency 결정을 기록합니다. 새 ADR은 다음 번호로 추가하고 context, 결정, 검토한 대안, 결과와 trade-off를 포함합니다.

| 번호 | 제목 | 상태 |
|---|---|---|
| [0001](0001-guild-scope.md) | 채팅방(길드) 스코프를 `botId`로 통일하고 로그인에 채팅방 선택 단계를 둔다 | Accepted (사후 기록) |
| [0002](0002-admin-api-prefix.md) | 관리자 API 경로를 `/api/admin/**`로 통일 | Accepted |
| [0003](0003-penalty-log.md) | 벌점 이력과 재전송 처리 | Active |
| [0004](0004-notification.md) | 알림 저장·실시간 전달 구조 (DB inbox + Redis Pub/Sub + SSE) | Accepted |
