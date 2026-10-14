package com.help.erpweb.domain.modules.chatbot.learning.dto.request.repository;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearningDto {

    @NotBlank(message = "ln_key는 필수입니다.")
    private String lnKey;

    @NotBlank(message = "ln_value는 필수입니다.")
    private String lnValue;

    /*
     * API에서는 Discord ID를 사용한다.
     * 실제 DB에는 Stored Procedure에서 SK로 변환하여 저장한다.
     */
    @NotBlank(message = "teacherDiscordId는 필수입니다.")
    private String teacherDiscordId;

    /** @deprecated ADR-0008 호환 필드. FE가 teacherDiscordId(으)로 전환한 뒤 제거한다. */
    @Deprecated
    @JsonProperty("teacher")
    public String getLegacyTeacher() {
        return teacherDiscordId;
    }

    /** @deprecated ADR-0008 호환 필드. 이전 이름(teacher)으로 들어온 요청을 받는다. */
    @Deprecated
    @JsonProperty("teacher")
    public void setLegacyTeacher(String value) {
        this.teacherDiscordId = value;
    }
}
