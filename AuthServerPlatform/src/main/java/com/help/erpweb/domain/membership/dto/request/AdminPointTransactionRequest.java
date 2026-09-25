package com.help.erpweb.domain.membership.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminPointTransactionRequest(
        @NotNull TransactionType type,
        @NotNull @Min(1) @Max(100000000) Integer amount,
        @NotBlank @Size(max = 255) String description
) {
}
