package com.help.erpweb.domain.modules.erp.point.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AdminPointDetailResponse(
        String discordId,
        String displayName,
        String state,
        int coin,
        List<AdminPointLogResponse> logs,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
    /** @deprecated ADR-0008 호환 필드. FE가 displayName(으)로 전환한 뒤 제거한다. */
    @Deprecated
    @JsonProperty("username")
    public String legacyUsername() {
        return displayName;
    }
}
