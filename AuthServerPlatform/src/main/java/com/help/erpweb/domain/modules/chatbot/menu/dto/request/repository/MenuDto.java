package com.help.erpweb.domain.modules.chatbot.menu.dto.request.repository;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuDto {
    @NotBlank(message = "mn_value는 필수입니다.")
    private String mnValue;

    @NotBlank(message = "recommenderDiscordId는 필수입니다.")
    private String recommenderDiscordId;

    /** @deprecated ADR-0008 호환 필드. FE가 recommenderDiscordId(으)로 전환한 뒤 제거한다. */
    @Deprecated
    @JsonProperty("recommender")
    public String getLegacyRecommender() {
        return recommenderDiscordId;
    }

    /** @deprecated ADR-0008 호환 필드. 이전 이름(recommender)으로 들어온 요청을 받는다. */
    @Deprecated
    @JsonProperty("recommender")
    public void setLegacyRecommender(String value) {
        this.recommenderDiscordId = value;
    }
}
