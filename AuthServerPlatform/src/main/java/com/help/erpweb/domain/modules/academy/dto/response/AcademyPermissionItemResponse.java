package com.help.erpweb.domain.modules.academy.dto.response;

import java.util.List;

public record AcademyPermissionItemResponse(
        Integer academyId,
        String role,
        List<Integer> classIds
) {
}
