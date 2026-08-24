package com.help.erpweb.domain.academy.dto.response;

import java.util.List;

public record StudentStatusResponse(
        List<AcademyOptionResponse> academies,
        List<ClassOptionResponse> classes,
        List<StudentOptionResponse> students,
        List<SubjectOptionResponse> subjects
) {
    public StudentStatusResponse {
        academies = academies != null ? academies : List.of();
        classes = classes != null ? classes : List.of();
        students = students != null ? students : List.of();
        subjects = subjects != null ? subjects : List.of();
    }
}