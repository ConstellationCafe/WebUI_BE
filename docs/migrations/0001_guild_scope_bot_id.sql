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
-- search_sk 저장 함수는 별도 저장소(디스코드 봇)에서 관리되므로 이 스크립트에는
-- 포함하지 않음. 시그니처가 search_sk(bot_id, cardType, membershipId)로
-- 바뀌어 있어야 한다.

-- 1) DiscordUsers: discordID 단독 PK를 surrogate key(id)로 교체하고,
--    bot_id 컬럼을 추가한다. discordID는 더 이상 전역 유일하지 않다 —
--    (bot_id, discordID) 조합으로만 유일하며, 같은 사람이 여러 방에 각각
--    별도 행(및 별도 역할)을 가질 수 있다.
--
--    주의: 이 id는 DiscordUsers 자체의 JPA @Id 용도로만 쓴다.
--    RoleTable은 이 id를 참조하지 않는다(아래 1-1 참고) — discordID
--    컬럼은 그대로 남긴다. 디스코드 봇이 여전히 discordID로 role을
--    기록하므로, 그 컬럼을 지우면 봇이 깨진다.
ALTER TABLE Constellation_Network.DiscordUsers
    ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT UNIQUE FIRST;
-- 기존 PK가 discordID(VARCHAR) 단독이었다면 아래 순서로 PK를 교체한다:
-- ALTER TABLE Constellation_Network.DiscordUsers DROP PRIMARY KEY;
-- ALTER TABLE Constellation_Network.DiscordUsers ADD PRIMARY KEY (id);

ALTER TABLE Constellation_Network.DiscordUsers
    ADD COLUMN bot_id VARCHAR(30) NOT NULL DEFAULT '';
-- 백필/DEFAULT 제거/UNIQUE 제약은 3), 4)에서 처리.

-- 1-1) RoleTable: discordID 컬럼은 유지(봇이 그 컬럼으로 role을 쓰고
--      있으므로 드롭하지 않음). 대신 DiscordUsers와 동일하게 bot_id
--      컬럼을 추가해서, (bot_id, discordID) 조합으로 DiscordUsers와
--      맞춘다 — surrogate id를 참조하는 FK는 만들지 않는다.
--
--      *** 중요: 이건 DB 스키마 변경만으로는 완성되지 않는다. ***
--      디스코드 봇(별도 저장소)이 RoleTable에 role을 INSERT/UPDATE할 때
--      bot_id도 같이 기록하도록 봇 코드를 고쳐야 한다. 안 그러면 이
--      컬럼은 항상 비어 있거나(또는 한 값으로만 채워져) 방 단위 admin
--      구분이 실제로는 이루어지지 않는다 — 지금 WebUI_BE 리팩토링의
--      핵심 목적(admin 여부를 방 단위로 재정의)이 무효화된다.
ALTER TABLE Constellation_Network.RoleTable
    ADD COLUMN bot_id VARCHAR(30) NOT NULL DEFAULT '';
-- 백필/DEFAULT 제거/인덱스는 3), 4)에서 DiscordUsers·Users와 함께 처리.

-- 2) Users: sk가 이제 (bot_id, discordID) 단위로 스코프되므로 bot_id 컬럼 필요.
--    discordID만으로는 더 이상 행이 유일하지 않다.
ALTER TABLE Constellation_Network.Users
    ADD COLUMN bot_id VARCHAR(30) NOT NULL DEFAULT '';

-- 3) 백필: 기존에 운영 중이던 단일 길드의 bot_id로 채운다.
--    :existing_bot_id를 실제 값으로 치환해서 실행.
-- UPDATE Constellation_Network.DiscordUsers SET bot_id = :existing_bot_id WHERE bot_id = '';
-- UPDATE Constellation_Network.RoleTable SET bot_id = :existing_bot_id WHERE bot_id = '';
-- UPDATE Constellation_Network.Users SET bot_id = :existing_bot_id WHERE bot_id = '';

-- 4) 백필 완료 후 DEFAULT 제거 + 인덱스/제약 정리
-- ALTER TABLE Constellation_Network.DiscordUsers ALTER COLUMN bot_id DROP DEFAULT;
-- ALTER TABLE Constellation_Network.DiscordUsers
--     ADD UNIQUE KEY uk_discord_users_bot_discord (bot_id, discordID);
-- ALTER TABLE Constellation_Network.RoleTable ALTER COLUMN bot_id DROP DEFAULT;
-- ALTER TABLE Constellation_Network.RoleTable
--     ADD INDEX idx_roletable_bot_discord (bot_id, discordID);
-- ALTER TABLE Constellation_Network.Users ALTER COLUMN bot_id DROP DEFAULT;
-- ALTER TABLE Constellation_Network.Users
--     ADD INDEX idx_users_bot_discord (bot_id, discordID);

-- 5) 검증
--    - 모든 DiscordUsers/Users/RoleTable 행의 bot_id가 빈 문자열이 아닌지 확인
--    - erp_subscriber에 등록된 bot_id와 일치하는지 확인
-- SELECT COUNT(*) FROM Constellation_Network.DiscordUsers WHERE bot_id = '';
-- SELECT COUNT(*) FROM Constellation_Network.Users WHERE bot_id = '';
-- SELECT COUNT(*) FROM Constellation_Network.RoleTable WHERE bot_id = '';
