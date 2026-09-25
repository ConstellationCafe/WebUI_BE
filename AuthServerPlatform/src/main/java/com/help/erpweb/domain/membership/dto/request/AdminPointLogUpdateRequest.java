package com.help.erpweb.domain.membership.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record AdminPointLogUpdateRequest(
        @Min(-100000000) @Max(100000000) Integer amount,
        @Size(max = 255) String description
) {
    @JsonIgnore
    @AssertTrue(message = "금액 또는 내역을 입력해 주세요.")
    public boolean isChangePresent() {
        return amount != null || description != null;
    }

    @JsonIgnore
    @AssertTrue(message = "금액은 0일 수 없습니다.")
    public boolean isAmountNonZero() {
        return amount == null || amount != 0;
    }
}
