package com.help.erpweb.domain.membership.dto.response;

import java.util.List;

public record AdminPointDetailResponse(
        String discordId,
        String username,
        String state,
        int coin,
        List<AdminPointLogResponse> logs,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
}
