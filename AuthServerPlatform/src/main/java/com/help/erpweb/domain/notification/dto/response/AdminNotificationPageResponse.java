package com.help.erpweb.domain.notification.dto.response;

import java.util.List;

public record AdminNotificationPageResponse(
        List<AdminNotificationResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
}
