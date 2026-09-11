package com.help.erpweb.domain.academy.dto.response;

import java.util.List;

public record AcademyPermissionResponse(
        boolean admin,
        List<AcademyPermissionItemResponse> academies
) {
}