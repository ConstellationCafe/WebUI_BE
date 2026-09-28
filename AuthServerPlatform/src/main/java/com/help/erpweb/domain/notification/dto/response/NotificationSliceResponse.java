package com.help.erpweb.domain.notification.dto.response;

import java.util.List;

/**
 * ID 커서 기반 목록. 다음 페이지는 {@code beforeId=nextBeforeId}로 요청한다.
 */
public record NotificationSliceResponse(
        List<NotificationResponse> items,
        boolean hasNext,
        Long nextBeforeId,
        long lastReadId
) {
}
