package com.help.erpweb.domain.notification.entity;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class NotificationReadCursorId implements Serializable {
    private String botId;
    private String discordId;
}
