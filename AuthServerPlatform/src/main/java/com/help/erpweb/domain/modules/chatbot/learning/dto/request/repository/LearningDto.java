package com.help.erpweb.domain.modules.chatbot.learning.dto.request.repository;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearningDto {

    @NotBlank(message = "ln_keyë íììëë¤.")
    private String lnKey;

    @NotBlank(message = "ln_valueë íììëë¤.")
    private String lnValue;

    /*
     * APIììë Discord IDë¥¼ ì¬ì©íë¤.
     * ì¤ì  DBìë Stored Procedureìì SKë¡ ë³ííì¬ ì ì¥íë¤.
     */
    @NotBlank(message = "teacherë íììëë¤.")
    private String teacher;
}
