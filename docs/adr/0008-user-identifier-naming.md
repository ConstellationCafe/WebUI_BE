# ADR-0008: 사용자 식별자와 표시 이름을 discordId·sk·displayName으로 구분해 부른다

- 상태: Accepted
- 날짜: 2026-10-14
- 적용 범위: WebUI_BE 코드·API 필드·JWT claim, WebUI_FE DTO·상태
- 담당자: 미정 (프로젝트 문서 담당자 지정 필요)
- 관련: [ADR-0001](0001-guild-scope.md), [API 개요](../api.md)

## Context

`username`이라는 이름이 서로 다른 세 값을 가리켰다.

- Spring Security principal·JWT `username` claim·Redis 세션 키: **Discord ID**
- `DiscordUsers.username` 컬럼과 이를 읽는 응답(벌점·포인트의 `username`): **서버 표시 이름** (빗자루 봇이 `member.display_name`을 기록)
- `/auth/me`의 `username`: **Discord 계정 username** (Discord API 값)

그 밖에도 `DiscordUser.getUsername()`이 Discord ID를, `getNickname()`이 `username` 컬럼을 돌려주는 등 이름과 값이 반대였고, `LessonRecord.mainTeacherId`·챗봇 엔티티 `teacher`처럼 이름은 ID인데 값은 SK인 필드, JWT에 항상 `null`로 실리던 `nickname` claim이 있었다. 같은 표시 이름이 API마다 `username`·`targetUsername`·`winnerName`·`name`으로 달랐다.

## Decision

1. 용어를 다음으로 고정한다.
   - `discordId`: Discord 사용자 snowflake ID. 역할이 붙으면 `<역할>DiscordId`
   - `sk`: 채팅방 단위 회원 식별자 `(botId, discordId)`. 역할이 붙으면 `<역할>Sk`
   - `displayName`: 서버 표시 이름. 역할이 붙으면 `<역할>DisplayName`
   - `discordUsername`, `globalName`: Discord 계정 username과 전역 표시 이름
   - 사람이 아닌 대상(Academy, 과목, 채널)의 이름은 `name`
2. Java 필드·변수·메서드와 API 필드를 이 용어로 바꾼다. **DB 컬럼 이름은 바꾸지 않고** `@Column(name = ...)`으로 연결한다(`DiscordUsers.username`, `discordID`, `main_teacher_id` 등은 봇과 공유).
3. JWT 사용자 claim을 `username`에서 `discordId`로 바꾸고 `nickname` claim을 없앤다. 배포 전 RefreshToken(수명 1일)을 위해 서버는 `username` claim도 읽는다.
4. Redis 세션(`SessionInfo`)은 필드 이름을 `discordId`로 바꾸되 `@JsonAlias("username")`으로 기존 세션을 읽는다.
5. API 필드 변경은 expand-and-contract로 한다. 응답은 새 이름과 이전 이름을 함께 내려주고, 요청은 두 이름을 모두 받는다. FE 전환 배포 뒤 이전 이름을 제거한다.

### API 필드 대응표

| API | 이전 | 새 이름 |
|---|---|---|
| `/auth/me` | `username` | `discordUsername` (FE 미사용이라 호환 필드 없음) |
| Academy 학생·강사·옵션 | `discordID`, `name` | `discordId`, `displayName` |
| Academy 수업 기록 목록 응답 | `mainTeacherName` | `mainTeacherDisplayName` |
| Academy 수업 기록 작성 요청 | `mainTeacherId`, `coTeacherIds`, `memberIds` | `mainTeacherSk`, `coTeacherSks`, `memberSks` |
| Academy 수업 기록 목록 쿼리 | `teacherId` | `teacherSk` |
| 벌점 상세·순위 | `username` | `displayName` |
| 벌점 이력 | `targetUsername` | `targetDisplayName` |
| 관리자 포인트 회원 | `username` | `displayName` |
| 대회 우승 | `winnerName` | `winnerDisplayName` |
| 챗봇 Content·Menu·Music | `recommender` | `recommenderDiscordId` |
| 챗봇 Learning | `teacher` | `teacherDiscordId` |

목록 검색·정렬에 쓰는 컬럼 이름(`recommender`, `teacher`)은 metadata 테이블 값이라 바꾸지 않는다.

## 검토한 대안

- **기존 API는 두고 새 API부터 규칙 적용**: FE·BE 변경이 적지만 같은 값을 부르는 이름이 계속 섞여 남는다.
- **DB 컬럼까지 변경**: 봇(ModularDiscordBot)과 공유하는 테이블이라 두 저장소 migration과 배포 순서 조율이 필요하다. 이번 범위에서 제외했다.
- **API 필드를 한 번에 교체(호환 필드 없음)**: BE·FE 배포 사이에 화면이 깨진다.

## 호환성과 운영 영향

- DB schema 변경 없음. 배포 순서: WebUI_BE → WebUI_FE. BE만 배포된 상태에서도 이전 FE는 이전 필드 이름으로 동작한다.
- 배포 직후 로그인 중인 사용자는 기존 RefreshToken과 Redis 세션으로 그대로 이어진다.
- rollback: BE를 되돌리면 새 FE가 새 필드를 못 받으므로 FE도 함께 되돌린다.

## 후속 작업

- [ ] WebUI_FE 전환 배포 확인 후 응답의 이전 필드, 요청 alias, `teacherId` 쿼리 제거
- [ ] 배포 1일 뒤 JWT `username` claim 읽기(`JwtUtil.LEGACY_DISCORD_ID_CLAIM`) 제거
- [ ] Redis 세션 TTL이 지난 뒤 `SessionInfo`의 `@JsonAlias("username")` 제거
- [ ] 문서 담당자 지정
