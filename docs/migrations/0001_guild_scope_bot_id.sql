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
-- 적용 범위: Constellation_Network 스키마 (DiscordUsers, Users)
-- search_sk 저장 함수는 별도 저장소(디스코드 봇)에서 관리되므로 이 스크립트에는
-- 포함하지 않음. 시그니처가 search_sk(bot_id, cardType, membershipId)로
-- 바뀌어 있어야 한다.

-- 1) DiscordUsers: (bot_id, discordID) 자연키로 확장
--    기존 PK가 discordID 단독이었다면, bot_id를 포함하는 복합 PK/유니크로 변경.
ALTER TABLE Constellation_Network.DiscordUsers
    ADD COLUMN bot_id VARCHAR(30) NOT NULL DEFAULT '';
-- 백필 후 DEFAULT ''는 제거하고 NOT NULL만 유지할 것.
-- ALTER TABLE Constellation_Network.DiscordUsers ALTER COLUMN bot_id DROP DEFAULT;

-- 2) Users: sk가 이제 (bot_id, discordID) 단위로 스코프되므로 bot_id 컬럼 필요.
--    discordID만으로는 더 이상 행이 유일하지 않다.
ALTER TABLE Constellation_Network.Users
    ADD COLUMN bot_id VARCHAR(30) NOT NULL DEFAULT '';

-- 3) 백필: 기존에 운영 중이던 단일 길드의 bot_id로 채운다.
--    :existing_bot_id를 실제 값으로 치환해서 실행.
-- UPDATE Constellation_Network.DiscordUsers SET bot_id = :existing_bot_id WHERE bot_id = '';
-- UPDATE Constellation_Network.Users SET bot_id = :existing_bot_id WHERE bot_id = '';

-- 4) 백필 완료 후 인덱스/제약 정리
-- ALTER TABLE Constellation_Network.DiscordUsers
--     ADD UNIQUE KEY uk_discord_users_bot_discord (bot_id, discordID);
-- ALTER TABLE Constellation_Network.Users
--     ADD INDEX idx_users_bot_discord (bot_id, discordID);

-- 5) 검증
--    - 모든 DiscordUsers/Users 행의 bot_id가 빈 문자열이 아닌지 확인
--    - erp_subscriber에 등록된 bot_id와 일치하는지 확인
-- SELECT COUNT(*) FROM Constellation_Network.DiscordUsers WHERE bot_id = '';
-- SELECT COUNT(*) FROM Constellation_Network.Users WHERE bot_id = '';
