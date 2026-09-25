# ADR-0001: 멀티 길드(테넌시) 지원을 위한 스코프 키와 sk 재정의

> 상태: Proposed
> 담당자: 미정 (프로젝트에서 지정 필요)
> 마지막 검토일: 2026-09-25
> 적용 범위: WebUI_BE, WebUI_FE, 및 Constellation_Network 스키마를 공유하는 봇 리포지토리(DiscordBot 등)

## Context (배경)

WebUI_BE는 두 개의 데이터소스를 사용한다.

- **ConfigDB** (`erp_subscriber`, `module_config`, `bots`, `bot_env` 등): 이미 `bot_id` 단위로 guild-scoped 되어 있다. `bot_env.guild_id`는 UNIQUE이며 `bot_id`가 PK이므로, **하나의 봇은 정확히 하나의 길드(채팅방)만 관리한다는 1:1 관계**가 스키마상 보장되어 있다. 이 서비스는 "채팅방 관리 봇"이라는 제품 성격상 이 1:1 관계는 의도적으로 유지한다(폐기하지 않음).
- **Constellation_Network** (`Users`, `CoinTable`, `PayLog`, `RecommendContent` 등): 길드 개념이 전혀 없다. `AdminPointRepository`의 native query는 `Constellation_Network.CoinTable`처럼 스키마명을 하드코딩하고 있고, 사용자 식별은 전역 `discordID` ↔ `sk` 1:1 매핑(`Constellation_Network.search_sk(cardType, membershipId)` 저장 함수)에 의존한다.

현재는 운영 중인 길드가 하나뿐이라 이 전역 매핑이 문제되지 않지만, **운영 길드가 향후 늘어날 예정**이므로 Constellation_Network 도메인도 길드(테넌트) 단위로 분리해야 한다.

### 검토한 대안

1. **모든 테이블에 `guildId` 컬럼 추가**: `CoinTable`, `PayLog`, `RecommendContent`, `RecommendMenu`, `RecommendMusic`, `Learning` 등 `sk`를 참조하는 모든 테이블에 개별적으로 `guildId`를 추가하고 모든 쿼리에 조건을 추가하는 방식. 스키마 변경 범위가 가장 크다.
2. **네이밍만 바꾸는 추상화 (Discord-agnostic 이름만 부여)**: 실제 멀티테넌시 없이 `ChatRoom`/`User`라는 이름만 도입. 근본적인 다중 길드 지원 요구를 충족하지 못해 채택하지 않음.
3. **(채택) `sk`를 테넌트 스코프 단위로 재정의**: `Users`/`DiscordUsers`의 자연키를 `(bot_id, discordId)`로 바꾸고, 그 결과로 생성되는 `sk`가 이미 해당 봇(=해당 길드)에 스코프된 값이 되도록 한다. `sk`를 FK처럼 참조만 하는 하위 테이블(`CoinTable`, `PayLog` 등)은 스키마 변경이 필요 없다 — guild 스코프가 `sk`를 통해 전이(transitive)되기 때문이다.

## Decision (결정)

- **스코프 키는 `bot_id`로 한다.** `bot_env.guild_id`가 UNIQUE이고 `bot_id`가 PK이므로 `bot_id`와 `guild_id`는 1:1이다. 요청 헤더로는 `guild_id`(외부 식별자, 사용자가 `/select`에서 선택하는 값)를 받지만, 애플리케이션 내부 전파 및 DB 스코프 단위는 `bot_id`로 통일한다.
- **`Constellation_Network.search_sk(cardType, membershipId)` 저장 함수와 `Users`/`DiscordUsers` 테이블은 `bot_id`를 포함하는 시그니처/키로 변경한다.** 이 변경은 WebUI_BE가 아닌 Constellation_Network 스키마를 공유하는 저장소(봇 리포지토리) 쪽에서 수행하며, WebUI_BE는 변경된 시그니처에 맞춰 `MembershipRepository`의 native query를 갱신한다.
- **카카오 등 다른 provider는 이번 변경 범위에서 제외한다.** `KakaotalkUsers` 테이블/로그인은 현재 존재하지 않으며, 필요 시 동일 패턴(`(bot_id, providerUserId)` 자연키)으로 추후 추가한다.
- **guildId → botId 해석은 요청 필터에서 수행한다.** 기존 `ERPSubscriberRepository.findByGuildId()`를 그대로 사용하며, 별도 resolver 컴포넌트를 새로 만들지 않는다. `/auth/**`를 제외한 모든 API 요청은 길드(채팅방) ID를 헤더로 전달해야 하며, 필터가 이를 `bot_id`로 변환해 요청 컨텍스트에 심는다.
- **헤더 누락 또는 미등록 guildId는 403으로 응답한다.** 이를 위한 전용 예외 클래스와 예외 핸들러 매핑이 필요하다(현재 `IllegalArgumentException`은 매핑이 없어 500으로 새는 문제가 있음).
- **Admin 권한은 길드 단위로 스코프된다.** 관리자가 다른 회원의 데이터를 조회/수정하는 API(`AdminPointController` 등)도 요청 헤더의 guildId(=관리자가 현재 보고 있는 채팅방)로 스코프되며, 다른 길드의 회원 데이터에는 접근할 수 없다. 즉 discordId 경로 파라미터는 항상 "현재 요청 헤더의 bot_id 안에서의" discordId로 해석된다.
- **`X-Room-Id`(가칭) 헤더를 신규 도입하고, `SecurityConfig`의 CORS `allowedHeaders`에 추가한다.**
- **FE의 `CurrentUserState`와 `CurrentGuildState`는 `ChatRoom`/`User` 추상화 도입 시점에 하나의 상태/notifier로 통합한다.**
- **DB 마이그레이션 관리**: 현재 리포지토리에는 Flyway/Liquibase 등 마이그레이션 도구와 변경 이력 파일이 존재하지 않는다(MySQL에 수동 DDL 적용). 이번 스키마 변경부터는 최소한 `docs/migrations/`에 순서가 있는 SQL 스크립트를 커밋해 변경 이력을 추적한다. 정식 마이그레이션 도구(Flyway 등) 도입 여부는 별도로 결정한다.

## Consequences (결과와 trade-off)

- **장점**: `CoinTable`, `PayLog`, `RecommendContent`, `RecommendMenu`, `RecommendMusic`, `Learning` 등 하위 테이블은 스키마 변경이 필요 없다. 테넌트 스코프의 단일 진실 공급원(`sk`)이 한 곳에만 존재해 정규화 관점에서 유리하다.
- **단점 / 위험**:
  - 물리적으로 동일한 Discord 계정이 여러 길드에 속하면 길드마다 별도의 `sk`(별도 경제/데이터)를 갖게 된다. 이는 "커뮤니티별 완전 독립"이라는 의도와 일치하지만, 길드 횡단 집계(예: 한 사람의 전체 누적 재화)는 이 구조에서 지원하지 않는다.
  - `discordId → sk` 역방향 조회가 있는 모든 지점(`AcademyService.toOptionResponse`, `findDiscordIdsBySk` 등)은 이제 `bot_id`를 함께 받아야 한다 — 이는 애플리케이션 코드 전반의 수정이 필요함을 의미하며, "테이블에 컬럼을 안 늘려도 된다"는 이점이 코드 수정량 감소로 곧바로 이어지지는 않는다.
  - `search_sk` 및 관련 스키마 변경은 WebUI_BE 리포지토리 밖(봇 리포지토리)에서 이뤄져야 하므로, 배포 순서를 조율해야 한다(봇 쪽 변경 → WebUI_BE 배포).
  - 기존 데이터 백필이 필요하다: 현재 존재하는 `sk` 값들을 현재 운영 중인 길드의 `bot_id`로 백필한다.
- **Breaking change**: `search_sk` 시그니처 변경 및 `Users`/`DiscordUsers` 키 변경은 breaking change다. 이를 사용하는 모든 consumer(WebUI_BE, 디스코드 봇, 기타 내부 API 소비자)의 마이그레이션 범위와 배포 순서를 별도 PR/ADR에 문서화해야 한다.

## 후속 결정 필요 항목

- Flyway/Liquibase 등 정식 마이그레이션 도구 도입 여부
- `search_sk` 및 관련 스키마 변경의 정확한 시그니처와 봇 리포지토리 쪽 작업 일정
- 기존 프로덕션 데이터 백필 절차와 검증 방법
- ADR 문서 담당자 및 검토 주기
