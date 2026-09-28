package com.help.erpweb.domain.notification.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** 이 ID까지(포함) 읽었다고 표시한다. 알림 패널에서 가장 최신으로 본 알림의 ID를 보낸다. */
public record NotificationReadCursorRequest(
        @NotNull @Min(0) Long lastReadId
) {
}
