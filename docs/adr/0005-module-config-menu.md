# ADR-0005: 채팅방 모듈 설정을 조회한 뒤 메뉴별 권한을 확인한다

- 상태: Accepted
- 날짜: 2026-09-30
- 적용 범위: WebUI_BE, WebUI_FE 메뉴와 로그인·채팅방 변경 흐름
- 담당자: 미정 (프로젝트 문서 담당자 지정 필요)
- 관련: [ADR-0001](0001-guild-scope.md), [API 개요](../api.md)

## Context

채팅방마다 봇의 활성 모듈이 다르지만 FE는 ChatBot·Shadowverse 메뉴를 항상 표시하고 아카데미·대회 권한을 모든 방에서 조회했다. config DB의 `module_config`가 실제 기능 사용 여부의 기준이다. 인증 쿠키는 HttpOnly이므로 FE가 JWT를 직접 해석하거나 방 식별자를 요청에 넣지 않는다.

## Decision

1. `GET /api/me/module-configs`는 기존 JWT 필터가 검증한 `GuildContext.botId`만 사용한다. 일반 인증 회원도 조회할 수 있다.
2. 메뉴에서 사용하는 세 모듈(`chatbot`, `shadowverse`, `network_operations`)만 모듈 ID 오름차순으로 조회한다. 복합 PK 기준 최대 3행이므로 pagination을 두지 않는다.
3. 원본 JSON 대신 `moduleId`와 메뉴용 `addOns` 이름 목록을 반환한다. `network_operations.config.add_on`의 `academy`·`competition`이 객체인 경우만 해당 메뉴가 활성화된다. 누락·null·잘못된 타입은 비활성으로 취급한다.
4. FE는 모듈을 조회한 뒤 활성화된 아카데미·대회만 기존 서버 권한 API를 조회한다. 교사·학원장·대회 매니저·서버장 판단은 서버가 유지한다.
5. 설정과 권한은 방 변경·로그아웃 때 함께 비우고, 이전 방에서 시작한 응답은 상태를 덮지 못하게 요청 세대로 구분한다. 설정 조회 실패는 모듈 메뉴를 숨기고 메뉴 영역에서 재시도한다.

## 검토한 대안

- 원본 ModuleConfig 전체 반환: 메뉴에 불필요한 채널·역할·회원 설정까지 클라이언트에 노출하므로 필요한 이름만 반환한다.
- FE의 JWT 디코딩 또는 요청 botId: HttpOnly 경계를 깨거나 다른 방을 지정할 입력을 만들므로 기존 검증된 요청 context를 재사용한다.
- 설정 없이 역할만 검사: 설정하지 않은 기능의 메뉴와 불필요한 권한 조회를 유지하므로 채택하지 않는다.

## 호환성과 운영 영향

신규 조회 API만 추가하며 기존 API 계약·DB 스키마는 바꾸지 않는다. BE를 먼저 배포하고 FE를 배포한다. FE만 먼저 배포되면 조회 실패 안내가 표시되며 모듈 메뉴가 숨겨진다. rollback은 FE를 먼저 이전 버전으로 되돌리고 필요하면 BE를 되돌린다. 설정 원문은 계속 서버에서만 읽는다.
