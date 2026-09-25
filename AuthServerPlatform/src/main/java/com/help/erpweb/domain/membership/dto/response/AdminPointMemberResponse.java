package com.help.erpweb.domain.membership.dto.response;

public record AdminPointMemberResponse(
        String discordId,
        String username,
        String state,
        int coin
) {
}
