package com.help.erpweb.domain.modules.erp.penalty.dto.response;

public record PenaltyMemberDetailResponse(
        String discordId,
        String username,
        String state,
        long cumulativeScore30d,
        PenaltyPageResponse<PenaltyHistoryItemResponse> history
) {
}
