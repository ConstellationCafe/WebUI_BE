package com.help.erpweb.domain.modules.academy.dto.response;

import java.util.List;

public record TeacherStatusResponse(
        List<AcademyOptionResponse> academies,
        List<ClassOptionResponse> classes,
        List<OptionResponse> teachers
) {

    public TeacherStatusResponse {

        academies = academies != null
                ? academies
                : List.of();

        classes = classes != null
                ? classes
                : List.of();

        teachers = teachers != null
                ? teachers
                : List.of();
    }
}
