package com.help.erpweb.domain.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원이 채팅방 단위로 어디까지 알림을 읽었는지 기록한다(ADR-0004).
 * 채팅방 전체 알림을 회원 수만큼 복제하지 않기 위해 알림별 읽음 행 대신
 * "마지막으로 읽은 알림 ID" 하나만 둔다. 이 ID보다 큰 알림이 읽지 않은 알림이다.
 * 쓰기는 {@code NotificationReadCursorRepository.advance}의 upsert로만 한다.
 */
@Entity
@Table(name = "NotificationReadCursor", schema = "Constellation_Network")
@IdClass(NotificationReadCursorId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationReadCursor {
    @Id
    @Column(name = "bot_id", nullable = false, length = 30)
    private String botId;

    @Id
    @Column(name = "discord_id", nullable = false, length = 20)
    private String discordId;

    @Column(name = "last_read_id", nullable = false)
    private long lastReadId;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
