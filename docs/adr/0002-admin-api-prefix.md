# ADR-0002: 관리자 API 경로를 `/api/admin/**`로 통일

- 상태: Accepted
- 날짜: 2026-09-28
- 적용 범위: WebUI_BE, WebUI_FE
- 담당자: 미정 (프로젝트 문서 담당자 지정 필요)
- 관련: ADR-0001(길드 스코프), `docs/api.md` 경로 규칙

## Context

- 관리자 포인트 API가 `/api/repository/membership/admin/points` 아래에 있었다. `repository`는 챗봇 모듈(content, learning, menu, music)의 저장소 편집 API에서 온 이름이고, 포인트 코드는 이미 `modules.erp.point` 패키지로 옮겨져 있어 URL과 코드 구조가 맞지 않았다.
- 관리자 검사는 컨트롤러마다 붙인 `@PreAuthorize` 하나에만 의존했다. 새 관리자 컨트롤러에서 어노테이션을 빠뜨리면 로그인한 일반 사용자에게 그대로 열린다.
- 벌점 API 등 관리자 기능이 계속 추가될 예정이라, 지금 경로 규칙을 정해 두지 않으면 같은 문제가 반복된다.

## Decision

1. 관리자 전용 API는 모두 `/api/admin/{resource}` 아래에 둔다. 예: `/api/admin/points`, `/api/admin/penalties`
2. `SecurityConfig.backendChain`에서 `/api/admin/**`을 `ROLE_ADMIN`으로 막는다. 컨트롤러의 `@PreAuthorize`는 그대로 유지해 이중으로 확인한다.
3. 인증은 됐지만 권한이 없는 요청은 `JsonAccessDeniedHandler`가 `404 NOT_FOUND` 래퍼로 응답한다. 기존 method authorization 거부(`GlobalExceptionHandler`)와 같은 동작이며, 관리자 API의 존재를 드러내지 않는다. 인증 실패는 기존처럼 `401`이다.
4. 새 API 경로는 kebab-case와 복수형 명사를 쓰고, 동작은 HTTP method로 표현한다. 내부 계층·패키지 이름(`repository`, `erp`, `modules`)은 경로에 넣지 않는다.
5. 버전 접두사(`/v1`)는 두지 않는다. 소비자가 같은 팀이 함께 배포하는 WebUI_FE 하나뿐이기 때문이다. 외부 클라이언트에 공개할 때 다시 결정한다.

## 이번에 옮긴 API

| 이전 | 이후 |
|---|---|
| `/api/repository/membership/admin/points/members` | `/api/admin/points/members` |
| `/api/repository/membership/admin/points/members/{discordId}` | `/api/admin/points/members/{discordId}` |
| `/api/repository/membership/admin/points/members/{discordId}/transactions` | `/api/admin/points/members/{discordId}/transactions` |
| `/api/repository/membership/admin/points/members/{discordId}/logs/{originalAmount}` (PATCH, DELETE) | `/api/admin/points/members/{discordId}/logs/{originalAmount}` |

## 호환성과 배포 순서

- 이전 경로는 호환 경로로 잠시 유지한다(`AdminPointController.DEPRECATED_BASE_PATH`). BE가 먼저 배포되어도 기존 FE가 계속 동작하고, FE가 먼저 배포되는 경우는 없도록 한다.
- 배포 순서: WebUI_BE 배포 → WebUI_FE 배포 → 이전 경로 호출이 없는지 로그 확인 → 이전 경로 제거 PR
- 호환 경로는 `/api/admin/**` URL 규칙 밖에 있으므로, 제거 전까지 컨트롤러의 `@PreAuthorize`가 유일한 관리자 검사다. 제거 전에는 이 어노테이션을 지우지 않는다.
- rollback: BE는 이전 경로가 살아 있으므로 FE만 되돌리면 된다. BE를 되돌리면 새 경로가 사라지므로, BE rollback 시에는 FE도 함께 되돌린다.

## 검토한 대안

- **`/api/erp/...`로 묶기**: ERP는 모듈 분류일 뿐 리소스가 아니다. 거의 모든 기능이 그 아래로 들어가 경로에 정보가 더해지지 않는다.
- **`/api/membership/...` 유지**: `membership`은 포인트를 감싸는 한 단계일 뿐 리소스를 설명하지 않고, 관리자 API를 URL 규칙으로 일괄 보호할 수 없다.
- **별도 관리자 서버·도메인 분리**: 보안상 가장 강하지만 현재 규모에서는 배포·인증 구성이 두 배가 된다. 관리자 기능이 커지면 다시 검토한다.

## Consequences

- 새 관리자 기능은 `/api/admin/**`에만 두면 URL 수준 보호를 자동으로 받는다.
- 일반 API(`/api/repository/...`, `/api/academy/...`)는 아직 이전 규칙을 따른다. 본인 조회용 `/api/me/...` 등으로의 정리는 별도 ADR·PR로 진행한다.
- FE는 한동안 두 경로가 모두 살아 있으므로, 신규 코드가 이전 경로를 쓰지 않도록 리뷰에서 확인한다.

## 후속 작업

- [ ] WebUI_FE 운영 배포 후 이전 경로 제거 PR (재검토 시점: FE 배포 후 1주)
- [ ] 문서 담당자 지정
