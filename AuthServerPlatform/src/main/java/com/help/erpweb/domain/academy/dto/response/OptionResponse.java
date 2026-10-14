package com.help.erpweb.domain.academy.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OptionResponse(
        String sk,
        String discordId,
        String displayName
) {
    /** @deprecated ADR-0008 호환 필드. FE가 discordId(으)로 전환한 뒤 제거한다. */
    @Deprecated
    @JsonProperty("discordID")
    public String legacyDiscordID() {
        return discordId;
    }

    /** @deprecated ADR-0008 호환 필드. FE가 displayName(으)로 전환한 뒤 제거한다. */
    @Deprecated
    @JsonProperty("name")
    public String legacyName() {
        return displayName;
    }
}
