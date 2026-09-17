package com.help.erpweb.domain.learning.projection;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LearningProjection {

    private String lnKey;

    private String lnValue;

    /*
     * DB의 teacher(SK)를 JOIN하여 얻은 Discord ID.
     * API에서는 이 값을 teacher로 사용한다.
     */
    private String teacher;
}