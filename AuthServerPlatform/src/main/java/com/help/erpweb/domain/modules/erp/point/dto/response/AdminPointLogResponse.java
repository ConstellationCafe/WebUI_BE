package com.help.erpweb.domain.modules.erp.point.dto.response;

import java.time.LocalDateTime;

public record AdminPointLogResponse(
        int amount,
        LocalDateTime at,
        String description
) {
}
