-- 0006: Competition.Winners에 채팅방 스코프 키(bot_id) 추가
--
-- 목적: 대회 우승 칭호를 채팅방(botId) 단위로 구분한다. WebUI_BE 대회 매니저가
--       현재 채팅방의 재적 회원에게 칭호를 부여한다(POST /api/competitions/winners).
-- 관련: ADR-0001(길드 스코프), docs/api.md "대회"
--
-- 전제(2026-09-30 기준 운영 DDL):
--   CREATE TABLE `Winners` (
--     `competition_name` varchar(100) NOT NULL,
--     `version` varchar(2) NOT NULL,
--     `winner` varchar(32) NOT NULL,
--     `acquisition` date NOT NULL,
--     PRIMARY KEY (`competition_name`,`version`,`winner`),
--     KEY `Winners_DiscordUsers_FK` (`winner`),
--     CONSTRAINT `Winners_DiscordUsers_FK` FOREIGN KEY (`winner`)
--       REFERENCES `Constellation_Network`.`DiscordUsers` (`discordID`) ON DELETE CASCADE ON UPDATE CASCADE
--   )
--   컬럼명·타입이 다르면 WebUI_BE 엔티티(CompetitionWinner)가 prod validate에서 기동하지 못한다.
--
-- 적용 순서: 이 파일 1) → 2) → 3) 순서로 수동 적용 → WebUI_BE 배포.
--   1)만 적용한 상태에서도 기존 쓰기(bot_id 없이 INSERT)는 DEFAULT ''로 계속 동작한다.
--   3)을 적용하기 전에는 다른 채팅방에서 같은 (대회명, 버전, 우승자)를 부여하면 기존 PK에 막힌다.
--
-- rollback:
--   3) 되돌리기: ALTER TABLE Competition.Winners DROP PRIMARY KEY,
--                ADD PRIMARY KEY (competition_name, version, winner);
--                (여러 채팅방에 같은 (대회명, 버전, 우승자)가 생겼다면 먼저 정리해야 한다)
--   1) 되돌리기: ALTER TABLE Competition.Winners DROP COLUMN bot_id;
--   WebUI_BE를 이전 버전으로 되돌려도 컬럼은 남겨 둘 수 있다.

-- 1) 컬럼 추가(맨 앞)
ALTER TABLE Competition.Winners
    ADD COLUMN bot_id VARCHAR(30) NOT NULL DEFAULT '' FIRST;

-- 2) 백필: 기존 행은 운영 중이던 채팅방의 bot_id로 채운다.
--    :existing_bot_id를 실제 값(config DB bots.bot_id)으로 치환해서 실행.
-- UPDATE Competition.Winners SET bot_id = :existing_bot_id WHERE bot_id = '';

-- 3) PK 교체: 채팅방마다 같은 대회명·버전·우승자를 따로 가질 수 있게 bot_id를 PK 앞에 둔다.
--    2) 백필을 끝낸 뒤 실행한다. FK 인덱스(Winners_DiscordUsers_FK)는 그대로 둔다.
ALTER TABLE Competition.Winners
    DROP PRIMARY KEY,
    ADD PRIMARY KEY (bot_id, competition_name, version, winner);

-- 검증
-- SELECT COUNT(*) FROM Competition.Winners WHERE bot_id = '';   -- 0이어야 함
-- SHOW CREATE TABLE Competition.Winners;                          -- PK가 (bot_id, competition_name, version, winner)
