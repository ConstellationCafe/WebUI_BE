-- ADR-0001: 멀티 길드 테넌시 스코프 키(bot_id) 도입
--
-- 이 스크립트는 Flyway/Liquibase 없이 변경 이력을 추적하기 위한
-- 참고용 문서다(docs/documentation.md, git-workflow.md 방침에 따름).
-- 실제 적용은 DBA/운영자가 직접 검증 후 수동으로 실행한다.
--
-- 전제: 이 리포지토리(WebUI_BE)의 코드(refactor/guild-scope-context 브랜치)는
-- 아래 스키마가 이미 적용되어 있다고 가정하고 작성되었다. 컬럼명/타입이
-- 실제와 다르면 해당 브랜치의 native query가 전부 깨지므로 반드시 대조할 것.
--
-- 적용 범위: Constellation_Network 스키마 (DiscordUsers, RoleTable, Users)
--           + search_sk_by_bot 저장 함수(신규, 기존 search_sk는 그대로 둔다)
--
-- 원칙: 디스코드 봇(별도 저장소)이 지금도 discordID 컬럼/search_sk(2-arg)로
-- 읽고 쓰고 있으므로, "이미 있는 걸 바꾸는" 방식이 아니라 "옆에 새로
-- 추가하는" 방식으로만 진행한다(컬럼 ADD, 프로시저는 별도 신규). 봇 쪽
-- 코드가 실제로 bot_id를 채워 넣기 전까지는 기존 동작이 그대로 보장된다.
--
-- ============================================================
-- ▶ 이미 실행됨 (2026-09-25 기준, 사용자가 직접 실행)
-- ============================================================
-- ALTER TABLE Constellation_Network.DiscordUsers
--     ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT UNIQUE FIRST;
-- ALTER TABLE Constellation_Network.DiscordUsers
--     ADD COLUMN bot_id VARCHAR(30) NOT NULL DEFAULT '' AFTER id;
-- ALTER TABLE Constellation_Network.RoleTable
--     ADD COLUMN discord_user_id BIGINT NULL AFTER discordID;
-- UPDATE Constellation_Network.RoleTable rt
--     JOIN Constellation_Network.DiscordUsers du ON du.discordID = rt.discordID
--     SET rt.discord_user_id = du.id;
--
-- 참고: RoleTable.discord_user_id는 이후 설계에서 더 이상 쓰지 않는다
-- (아래 1-1 참고 — DiscordUsers/RoleTable을 (bot_id, discordID)로 맞추는
-- 방식으로 정리했다). 이미 backfill된 값이라 굳이 지금 되돌릴 필요는
-- 없고, 단지 "안 쓰는 컬럼"으로 남는다. 나중에 원하면 DROP COLUMN
-- discord_user_id로 정리해도 된다(선택 사항, 지금 급하지 않음).

-- ============================================================
-- ▶ 남은 작업 (전부 additive라 봇 코드와 충돌하지 않음)
-- ============================================================

-- 1) RoleTable: discordID 컬럼은 유지(봇이 그 컬럼으로 role을 쓰고
--    있으므로 드롭하지 않음). DiscordUsers와 동일하게 bot_id 컬럼을
--    추가해서, (bot_id, discordID) 조합으로 DiscordUsers와 맞춘다.
ALTER TABLE Constellation_Network.RoleTable
    ADD COLUMN bot_id VARCHAR(30) NOT NULL DEFAULT '';

-- 2) Users: sk가 이제 (bot_id, discordID) 단위로 스코프되므로 bot_id 컬럼 필요.
--    discordID만으로는 더 이상 행이 유일하지 않다.
ALTER TABLE Constellation_Network.Users
    ADD COLUMN bot_id VARCHAR(30) NOT NULL DEFAULT '';

-- 3) 백필: 기존에 운영 중이던 단일 길드의 bot_id로 채운다.
--    :existing_bot_id를 실제 값으로 치환해서 실행.
-- UPDATE Constellation_Network.DiscordUsers SET bot_id = :existing_bot_id WHERE bot_id = '';
-- UPDATE Constellation_Network.RoleTable SET bot_id = :existing_bot_id WHERE bot_id = '';
-- UPDATE Constellation_Network.Users SET bot_id = :existing_bot_id WHERE bot_id = '';

-- 4) 백필 완료 후 DEFAULT 제거 + 인덱스 정리 (전부 additive, 여기까지는
--    봇 코드가 몰라도 안전 — 새 컬럼/인덱스일 뿐 기존 쿼리를 막지 않음)
-- ALTER TABLE Constellation_Network.DiscordUsers ALTER COLUMN bot_id DROP DEFAULT;
-- ALTER TABLE Constellation_Network.DiscordUsers
--     ADD UNIQUE KEY uk_discord_users_bot_discord (bot_id, discordID);
-- ALTER TABLE Constellation_Network.RoleTable ALTER COLUMN bot_id DROP DEFAULT;
-- ALTER TABLE Constellation_Network.RoleTable
--     ADD INDEX idx_roletable_bot_discord (bot_id, discordID);
-- ALTER TABLE Constellation_Network.Users ALTER COLUMN bot_id DROP DEFAULT;
-- ALTER TABLE Constellation_Network.Users
--     ADD INDEX idx_users_bot_discord (bot_id, discordID);

-- ============================================================
-- ▶ 여기서부터는 "SQL만으로는" 봇 코드와의 충돌을 피할 수 없는 지점
-- ============================================================
--
-- DiscordUsers의 PK를 discordID 단독에서 id로 교체하는 작업:
--
-- ALTER TABLE Constellation_Network.DiscordUsers DROP PRIMARY KEY;
-- ALTER TABLE Constellation_Network.DiscordUsers ADD PRIMARY KEY (id);
--
-- 이건 "컬럼을 옆에 추가"하는 게 아니라 "기존 유일성 규칙을 바꾸는"
-- 작업이라 성격이 다르다. 봇이 DiscordUsers에 INSERT할 때
-- discordID의 PK/UNIQUE 유일성에 기대는 패턴(예: INSERT ... ON DUPLICATE
-- KEY UPDATE, REPLACE INTO)을 쓰고 있다면, bot_id를 채우지 않는 INSERT는
-- 이 시점부터 실제 동작이 바뀔 수 있다(현재는 방이 하나뿐이라 당장
-- 터지진 않지만, 두 번째 길드가 생기는 순간부터 의미가 달라진다).
--
-- 이 ALTER는 "봇이 DiscordUsers INSERT에도 bot_id를 채우도록" 봇 코드가
-- 먼저 바뀐 뒤에 실행해야 한다. 프로시저처럼 "새로 하나 더 만들어서
-- 우회"할 수 있는 종류의 변경이 아니다 — PK는 테이블당 하나뿐이라서.
-- 봇 쪽 확인 전까지는 이 ALTER를 미뤄두는 걸 권장한다.

-- ============================================================
-- ▶ search_sk: 기존 함수를 바꾸지 않고 새 함수를 추가
-- ============================================================
--
-- 기존 search_sk(cardType, membershipId)는 봇 저장소가 그대로 호출하고
-- 있으므로 시그니처/DEFINER를 그대로 두고 손대지 않는다. 대신 bot_id까지
-- 받는 search_sk_by_bot을 별도 함수로 새로 추가한다. 로직은 기존
-- search_sk 정의(사용자 제공)에 u.bot_id 조건만 추가한 것과 동일하다.
--
-- WebUI_BE는 이 신규 함수만 호출하도록 이미 코드를 맞췄다
-- (MembershipRepository, ContentService, MenuService, MusicService,
-- LearningService의 findSkByDiscordId). 파라미터 이름은 bot_id로 두면
-- WHERE u.bot_id = bot_id가 컬럼/파라미터를 구분하지 못해 모호해지므로
-- p_bot_id로 바꿨다. DEFINER는 실제 DB 계정에 맞게 조정할 것.
CREATE DEFINER=`elaina`@`%` FUNCTION `Constellation_Network`.`search_sk_by_bot`(
	p_bot_id VARCHAR(30),
	card_type VARCHAR(10),
	membershipID VARCHAR(32)
) RETURNS varchar(36) CHARSET utf8mb4
    READS SQL DATA
BEGIN
	DECLARE result_sk VARCHAR(36);
    IF card_type = 'kakaotalk' THEN
        SELECT sk INTO result_sk
		FROM Constellation_Network.Users u
		WHERE u.kakaotalkID = membershipID
		  AND u.bot_id = p_bot_id;
    ELSE
        SELECT sk INTO result_sk
        FROM Constellation_Network.Users u
        WHERE u.discordID = membershipID
          AND u.bot_id = p_bot_id;
    END IF;
    RETURN result_sk;
END;
--
-- 검증: 기존 search_sk 호출부(봇 쪽)는 건드리지 않았으므로 회귀 테스트
-- 대상이 아니다. search_sk_by_bot만 새로 검증하면 된다.
-- SELECT Constellation_Network.search_sk_by_bot(:existing_bot_id, 'discord', :test_discord_id);
-- 기존 SELECT Constellation_Network.search_sk('discord', :test_discord_id)와 같은 sk가 나와야 한다.

-- ============================================================
-- ▶ 검증
-- ============================================================
--    - 모든 DiscordUsers/Users/RoleTable 행의 bot_id가 빈 문자열이 아닌지 확인
--    - erp_subscriber에 등록된 bot_id와 일치하는지 확인
-- SELECT COUNT(*) FROM Constellation_Network.DiscordUsers WHERE bot_id = '';
-- SELECT COUNT(*) FROM Constellation_Network.Users WHERE bot_id = '';
-- SELECT COUNT(*) FROM Constellation_Network.RoleTable WHERE bot_id = '';
