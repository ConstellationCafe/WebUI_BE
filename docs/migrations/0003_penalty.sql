-- PenaltyLog: expand-only migration. 운영자가 스키마 검증·백업 후 수동 적용.
-- 기존 테이블과 Discord 봇의 쓰기 경로는 변경하지 않는다.
-- 애플리케이션 배포 전에 테이블을 먼저 생성하고 DB 계정 권한을 확인한다.
-- DATETIME(3)은 애플리케이션이 UTC wall time으로 변환해 저장한다.
CREATE TABLE Constellation_Network.PenaltyLog (
    id                       BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    bot_id                   VARCHAR(30)  NOT NULL,
    sk                       VARCHAR(36)  NOT NULL,
    target_discord_id        VARCHAR(20)  NOT NULL,
    target_username          VARCHAR(100) NOT NULL,
    channel_id               VARCHAR(20)  NOT NULL,
    channel_name             VARCHAR(100) NULL,
    reason                   VARCHAR(255) NOT NULL,
    score                    INT          NOT NULL,
    issuer_discord_id        VARCHAR(20)  NOT NULL,
    occurred_at              DATETIME(3)  NOT NULL,
    requested_occurred_at    DATETIME(3)  NULL,
    created_at               DATETIME(3)  NOT NULL,
    status                   VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    request_id               CHAR(36)     NOT NULL,
    canceled_by_discord_id   VARCHAR(20)  NULL,
    canceled_at              DATETIME(3)  NULL,
    cancellation_reason      VARCHAR(255) NULL,
    CONSTRAINT ck_penalty_score CHECK (score BETWEEN 1 AND 100),
    UNIQUE KEY uk_penalty_request (bot_id, request_id),
    KEY ix_penalty_sk_time (sk, occurred_at),
    KEY ix_penalty_bot_time (bot_id, occurred_at, id),
    KEY ix_penalty_channel_time (bot_id, channel_id, occurred_at),
    KEY ix_penalty_window (bot_id, status, occurred_at, target_discord_id)
);
-- requested_occurred_at은 시간 입력 생략과 명시 입력을 구별해 재전송 내용을 비교한다.
-- 롤백은 API부터 이전 버전으로 되돌린 후, 운영 데이터 백업과 복구 가능성을
-- 확인한 경우에만 아래 명령을 실행한다.
-- DROP TABLE Constellation_Network.PenaltyLog;
