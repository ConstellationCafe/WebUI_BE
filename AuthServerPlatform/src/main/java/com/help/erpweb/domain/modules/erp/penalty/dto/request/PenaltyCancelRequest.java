package com.help.erpweb.domain.modules.erp.penalty.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PenaltyCancelRequest(@NotBlank @Size(max = 255) String reason) {
}
