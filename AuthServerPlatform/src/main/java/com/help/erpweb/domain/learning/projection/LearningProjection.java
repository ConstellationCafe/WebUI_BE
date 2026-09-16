package com.help.erpweb.domain.learning.projection;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LearningProjection {

    private String lnKey;
    private String lnValue;

    // 실제 DB 값
    private String teacher;

    // JOIN으로 조회한 화면 표시용 값
    private String teacherDiscordId;
}