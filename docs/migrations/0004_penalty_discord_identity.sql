-- 0003을 이미 적용한 환경과 신규 설치 환경 모두 0003 다음에 적용한다.
-- 벌점 대상은 (bot_id, target_discord_id)로 식별하며 Users.sk는 더 이상 필요하지 않다.
-- 백업과 기존 PenaltyLog 확인 후, sk 없는 애플리케이션을 배포하기 전에 수동 적용한다.
ALTER TABLE Constellation_Network.PenaltyLog
    DROP INDEX ix_penalty_sk_time,
    DROP COLUMN sk;

-- 기존 데이터는 bot_id와 target_discord_id 및 취소 감사 정보를 보존한다.
-- 롤백: 새 코드 배포 전에는 백업에서 복구 가능하다. 배포 후 이전 코드로
-- 되돌리려면 먼저 sk를 복원하고 기존 행에 올바른 sk를 채워야 한다.
