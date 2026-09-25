package com.help.erpweb.domain.membership.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.help.erpweb.domain.membership.dto.request.AdminPointLogUpdateRequest;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

class AdminPointLogUpdateRequestTest {
    @Test
    void requiresAtLeastOneChange() {
        var validator = Validation.buildDefaultValidatorFactory().getValidator();
        assertThat(validator.validate(new AdminPointLogUpdateRequest(null, null))).isNotEmpty();
        assertThat(validator.validate(new AdminPointLogUpdateRequest(null, "설명"))).isEmpty();
    }

    @Test
    void rejectsZeroAndOutOfRangeAmounts() {
        var validator = Validation.buildDefaultValidatorFactory().getValidator();
        assertThat(validator.validate(new AdminPointLogUpdateRequest(0, null))).isNotEmpty();
        assertThat(validator.validate(new AdminPointLogUpdateRequest(100000001, null))).isNotEmpty();
        assertThat(validator.validate(new AdminPointLogUpdateRequest(-100000001, null))).isNotEmpty();
        assertThat(validator.validate(new AdminPointLogUpdateRequest(-5800, null))).isEmpty();
    }
}
