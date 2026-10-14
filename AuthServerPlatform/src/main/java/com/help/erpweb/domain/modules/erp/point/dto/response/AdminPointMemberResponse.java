package com.help.erpweb.domain.modules.erp.point.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AdminPointMemberResponse(
        String discordId,
        String displayName,
        String state,
        int coin
) {
    /** @deprecated ADR-0008 호환 필드. FE가 displayName(으)로 전환한 뒤 제거한다. */
    @Deprecated
    @JsonProperty("username")
    public String legacyUsername() {
        return displayName;
    }
}
