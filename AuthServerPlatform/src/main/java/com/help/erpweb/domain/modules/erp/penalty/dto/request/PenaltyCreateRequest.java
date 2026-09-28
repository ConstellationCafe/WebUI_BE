package com.help.erpweb.domain.modules.erp.penalty.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PenaltyCreateRequest(
        @NotBlank @Pattern(regexp = "[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}") String requestId,
        @NotBlank @Pattern(regexp = "[0-9]{1,20}") String targetDiscordId,
        @NotBlank @Pattern(regexp = "[0-9]{1,20}") String channelId,
        @Size(max = 100) String channelName,
        @NotBlank @Size(max = 255) String reason,
        @NotNull @Min(1) @Max(1) Integer score,
        @Pattern(regexp = "[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}(\\.[0-9]{1,9})?Z")
        String occurredAt
) {
}
