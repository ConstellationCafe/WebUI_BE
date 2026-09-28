package com.help.erpweb.domain.modules.erp.point.dto.response;

public record AdminPointMemberResponse(
        String discordId,
        String username,
        String state,
        int coin
) {
}
