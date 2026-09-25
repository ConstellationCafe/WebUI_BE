package com.help.erpweb.domain.membership.dto.response;

import java.time.LocalDateTime;

public record AdminPointLogResponse(
        int amount,
        LocalDateTime at,
        String description
) {
}
