-- ADR-0004: 알림(Notification)과 회원별 읽음 위치(NotificationReadCursor)
--
-- expand-only migration이다. 새 테이블 두 개만 추가하고 기존 테이블과 Discord 봇의
-- 쓰기 경로는 바꾸지 않는다. 운영자가 스키마·백업·DB 계정 권한(INSERT/SELECT/UPDATE)을
-- 확인한 뒤 수동으로 적용하고, 그 다음 애플리케이션을 배포한다(테이블이 없으면 알림 API와
-- 포인트 입·출금 요청이 실패한다).
--
-- 시간 값은 애플리케이션이 UTC wall time으로 변환해 DATETIME(3)(밀리초)으로 저장한다.

CREATE TABLE Constellation_Network.Notification (
    id                 BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    bot_id             VARCHAR(30)   NOT NULL,
    target_type        VARCHAR(16)   NOT NULL,
    target_discord_id  VARCHAR(20)   NULL,
    category           VARCHAR(20)   NOT NULL,
    title              VARCHAR(100)  NOT NULL,
    body               VARCHAR(1000) NOT NULL,
    link               VARCHAR(255)  NULL,
    source             VARCHAR(16)   NOT NULL,
    source_ref         VARCHAR(64)   NOT NULL,
    request_key        VARCHAR(64)   NULL,
    created_at         DATETIME(3)   NOT NULL,
    CONSTRAINT ck_notification_target CHECK (
        (target_type = 'GUILD' AND target_discord_id IS NULL)
        OR (target_type = 'USER' AND target_discord_id IS NOT NULL)
    ),
    -- 재전송 멱등성. request_key가 NULL인 내부 발행은 서로 충돌하지 않는다.
    UNIQUE KEY uk_notification_request (bot_id, source, source_ref, request_key),
    -- 회원 목록/읽지 않은 개수: 채팅방 전체 알림과 본인 대상 알림을 id 순으로 찾는다.
    KEY ix_notification_bot_target (bot_id, target_type, target_discord_id, id),
    -- 관리자 발행 이력(최신순 페이지)
    KEY ix_notification_bot_id (bot_id, id)
);

CREATE TABLE Constellation_Network.NotificationReadCursor (
    bot_id        VARCHAR(30)  NOT NULL,
    discord_id    VARCHAR(20)  NOT NULL,
    last_read_id  BIGINT       NOT NULL,
    updated_at    DATETIME(3)  NOT NULL,
    PRIMARY KEY (bot_id, discord_id)
);

-- 보존 정책: 알림 보존 기간은 아직 정하지 않았다(README "관측성과 운영" 미정 항목).
-- 정해지면 created_at 기준 정리 작업을 별도 migration/runbook으로 추가한다.
--
-- 검증
-- SHOW CREATE TABLE Constellation_Network.Notification;
-- SHOW CREATE TABLE Constellation_Network.NotificationReadCursor;
-- EXPLAIN SELECT id FROM Constellation_Network.Notification
--   WHERE bot_id = :bot_id AND (target_type = 'GUILD' OR target_discord_id = :discord_id)
--   ORDER BY id DESC LIMIT 21;
--
-- 롤백: 애플리케이션을 이전 버전으로 먼저 되돌린다. 알림 데이터 백업과 복구 필요성을
-- 확인한 경우에만 아래 명령을 실행한다(알림·읽음 기록이 모두 사라진다).
-- DROP TABLE Constellation_Network.NotificationReadCursor;
-- DROP TABLE Constellation_Network.Notification;
