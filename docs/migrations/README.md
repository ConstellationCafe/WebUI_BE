# DB Migration

> 상태: Active  
> 마지막 검토일: 2026-09-28  
> 상위 문서: [README](../../README.md) · 관련: [deploy](../deploy.md), [ADR 목록](../adr/README.md)

Flyway/Liquibase를 쓰지 않습니다. 이 폴더의 SQL은 변경 이력이자 운영자가 수동으로 적용하는 스크립트입니다.

## 원칙

- **expand-only 우선**: 기존 컬럼·함수를 바꾸지 않고 옆에 추가합니다. 같은 DB를 쓰는 봇([ModularDiscordBot](https://github.com/ConstellationCafe/ModularDiscordBot))의 쓰기 경로를 깨지 않기 위해서입니다.
- **migration 먼저, 애플리케이션 나중**: prod profile은 `ddl-auto: validate`라 테이블이 없으면 기동하거나 요청을 처리하지 못합니다.
- 시간 값은 애플리케이션이 UTC wall time으로 변환해 `DATETIME(3)`(밀리초)으로 저장합니다.
- 새 migration은 다음 번호로 추가하고, 파일 머리 주석에 목적·전제·적용 순서·rollback 방법을 적습니다. 관련 ADR이 있으면 연결합니다.

## 목록과 적용 순서

| 순서 | 파일 | 내용 | 관련 | 비고 |
|---|---|---|---|---|
| 1 | [0001_guild_scope_bot_id.sql](0001_guild_scope_bot_id.sql) | `DiscordUsers`·`RoleTable`·`Users`에 `bot_id` 추가, `search_sk_by_bot` 함수 | [ADR-0001](../adr/0001-guild-scope.md) | 일부는 2026-09-25에 적용됨(파일 주석 참고). backfill과 PK 교체는 봇 변경 이후 |
| 2 | [0003_penalty.sql](0003_penalty.sql) | `PenaltyLog` 생성 | [ADR-0003](../adr/0003-penalty-log.md) | 이미 적용했다면 건너뜀 |
| 3 | [0004_penalty_discord_identity.sql](0004_penalty_discord_identity.sql) | `PenaltyLog.sk` 제거 | [ADR-0003](../adr/0003-penalty-log.md) | 0003 다음에 적용 |
| 4 | [0005_notification.sql](0005_notification.sql) | `Notification`, `NotificationReadCursor` 추가 | [ADR-0004](../adr/0004-notification.md) | 미적용 시 알림 API와 **관리자 포인트 입·출금이 실패** |

0002 번호는 사용하지 않았습니다(ADR-0002는 경로 규칙이라 schema 변경이 없음).

## 적용 절차

1. 운영 DB backup과 복구 가능 여부를 확인합니다.
2. 파일의 전제(현재 schema, 컬럼명·타입)를 운영 DB와 대조합니다.
3. DB 계정 권한(INSERT/SELECT/UPDATE, 함수 생성 시 DEFINER)을 확인합니다.
4. 순서대로 수동 적용하고, 파일 끝의 검증 query를 실행합니다.
5. 애플리케이션을 배포하고 `/actuator/health/readiness`를 확인합니다([deploy](../deploy.md)).

## rollback

- 새 테이블·컬럼만 추가한 migration(0003, 0005)은 애플리케이션을 되돌려도 남겨 둘 수 있습니다.
- 0004 적용 뒤 이전 코드로 되돌리려면 `PenaltyLog.sk` 컬럼과 기존 행의 값을 backup에서 먼저 복원해야 합니다.
- 0001의 PK 교체는 되돌리기 어려우므로 봇 변경을 확인하기 전에는 실행하지 않습니다.
- 대용량 데이터 영향과 인덱스·조회 계획은 운영 데이터 규모에서 확인합니다.
