package com.help.erpweb.domain.modules.erp.penalty.dto.response;

import java.time.Instant;

public record PenaltyMemberRankResponse(
        String discordId,
        String username,
        long cumulativeScore30d,
        long penaltyCount30d,
        Instant lastOccurredAt
) {
}
