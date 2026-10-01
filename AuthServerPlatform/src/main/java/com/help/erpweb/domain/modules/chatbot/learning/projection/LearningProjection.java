package com.help.erpweb.domain.modules.chatbot.learning.projection;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LearningProjection {

    private String lnKey;

    private String lnValue;

    /*
     * DBì teacher(SK)ë¥¼ JOINíì¬ ì»ì Discord ID.
     * APIììë ì´ ê°ì teacherë¡ ì¬ì©íë¤.
     */
    private String teacher;
}
