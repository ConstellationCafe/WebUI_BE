# ADR-0006: 벌점 관리를 운영 매니저·운영 본부원에게 열고 `/api/penalties`로 옮긴다

- 상태: Accepted
- 날짜: 2026-10-04
- 적용 범위: WebUI_BE 벌점 관리 API, WebUI_FE ERP 메뉴·벌점 관리 화면
- 담당자: 미정 (프로젝트 문서 담당자 지정 필요)
- 관련: [ADR-0001](0001-guild-scope.md)(길드 스코프), [ADR-0002](0002-admin-api-prefix.md)(관리자 경로), [ADR-0003](0003-penalty-log.md)(벌점 이력), [ADR-0005](0005-module-config-menu.md)(메뉴 권한), [API 개요](../api.md)

## Context

- 벌점 관리 API는 `/api/admin/penalties` 아래에 있어 `SecurityConfig`의 `/api/admin/**` → `ROLE_ADMIN` URL 규칙(ADR-0002)과 `@authorization.isAdmin`으로 서버장만 쓸 수 있었다.
- 운영에서는 서버장 외에도 디스코드 역할 `운영 매니저`, `운영 본부원`이 벌점을 부여·취소해야 한다.
- FE가 받는 사용자 역할은 `ROLE_USER`/`ROLE_ADMIN`뿐이라 RoleTable의 디스코드 역할명을 알 수 없다. 메뉴 표시 여부도 서버가 알려 줘야 한다.

## Decision

1. `PenaltyAuthorization`(`@penaltyAuth`)을 둔다. 현재 채팅방(`botId`)의 RoleTable에 `운영 매니저` 또는 `운영 본부원`이 **들어간** 역할이 있거나 서버장이면 관리 권한이 있다. 역할명 포함 비교는 대회 매니저(`@competitionAuth`)와 같은 방식이다(봇이 디스코드 역할명을 그대로 기록하므로 접두사 등이 붙을 수 있다).
2. 벌점 관리 API를 `/api/penalties/**`로 옮기고 `@PreAuthorize("@penaltyAuth.isManager(authentication)")`로 확인한다. 서버장 전용이 아니므로 `/api/admin/**` 밖에 둔다(대회 `/api/competitions`와 같은 결정). 권한이 없으면 기존 method authorization 거부와 같이 404다.
3. `GET /api/penalties/me/permissions` → `{manager}`를 추가한다. 로그인한 회원 누구나 호출하며, FE는 이 값으로 ERP 메뉴의 벌점 관리 항목과 화면 접근을 정한다. 최종 판단은 서버가 한다.
4. FE는 ERP 메뉴의 기능별 권한(포인트·알림 발행: 서버장, 벌점: 위 권한)을 확인하고, 볼 수 있는 기능이 하나도 없으면 ERP 카테고리를 숨긴다.

## 호환성과 배포 순서

- expand-and-contract: 같은 handler를 `/api/admin/penalties/**`에도 남겨 둔다(`PenaltyController.DEPRECATED_BASE_PATH`). 이 경로는 ADR-0002 URL 규칙 때문에 계속 서버장만 통과하므로, 구버전 FE를 쓰는 서버장은 배포 사이에도 기존처럼 쓸 수 있다.
- 배포 순서: WebUI_BE 배포 → WebUI_FE 배포 → 호환 경로 호출이 없는지 로그 확인 → 호환 경로 제거 PR. FE만 먼저 배포되면 새 경로가 없어 벌점 관리 화면과 권한 조회가 실패한다(권한 조회 실패 시 서버장 외에는 메뉴가 숨겨진다).
- rollback: 호환 경로가 남아 있는 동안에는 FE만 이전 버전으로 되돌려도 서버장 기능은 동작한다. BE를 되돌리면 새 FE의 벌점 관리 화면이 404를 받으므로 FE도 함께 되돌린다.
- DB 스키마 변경은 없다. 벌점 부여·취소 기록의 `issuerDiscordId`·`canceledByDiscordId`에는 운영 역할 회원의 Discord ID가 그대로 남는다.

## 검토한 대안

- **`/api/admin/penalties` 유지 + SecurityConfig 예외**: `/api/admin/**`이 "서버장 전용"이라는 규칙이 깨져, 어노테이션 누락 시 일반 사용자에게 열리는 문제(ADR-0002 Context)가 다시 생긴다.
- **`/api/me`나 사용자 응답에 디스코드 역할명 전체를 내려주고 FE가 판단**: 역할 목록이 클라이언트에 그대로 노출되고, 역할명 규칙이 FE·BE 두 곳에 생긴다. 권한 판단은 서버에 두고 결과만 내려준다.
- **역할명 완전 일치 비교**: 봇이 기록하는 실제 역할명에 접두사가 붙는 경우(예: `섀버 대회 매니저`)가 있어 대회와 같이 포함 비교를 쓴다. 대신 `운영 매니저`·`운영 본부원`이 들어간 다른 역할이 생기면 권한이 함께 열리므로, 역할명을 만들 때 주의한다.

## Consequences

- 운영 역할 회원이 서버장 없이 벌점을 관리할 수 있다. 포인트·알림 발행은 여전히 서버장 전용이다.
- 벌점 관리 권한 확인마다 RoleTable 조회가 한 번 일어난다(서버장은 조회하지 않음).
- 호환 경로가 남아 있는 동안 같은 기능이 두 경로로 열려 있으므로 리뷰에서 신규 코드가 이전 경로를 쓰지 않는지 확인한다.

## 후속 작업

- [ ] 새 FE 배포 후 `/api/admin/penalties/**` 호출이 없는지 확인하고 호환 경로 제거
- [ ] 문서 담당자 지정
