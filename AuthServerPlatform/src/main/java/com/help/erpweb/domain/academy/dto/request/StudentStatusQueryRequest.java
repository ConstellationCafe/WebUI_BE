package com.help.erpweb.domain.academy.dto.request;

import com.help.erpweb.domain.academy.type.StudentRosterStatus;

public record StudentStatusQueryRequest(
        Integer academyId,
        Integer classId,
        String studentId,
        StudentRosterStatus status,
        Integer page,
        Integer size
) {
    public int normalizedPage() {
        if (page == null || page < 1) {
            return 1;
        }

        return page;
    }

    public int normalizedSize() {
        if (size == null || size < 1) {
            return 20;
        }

        return size;
    }
}