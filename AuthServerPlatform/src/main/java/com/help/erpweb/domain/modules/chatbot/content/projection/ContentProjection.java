package com.help.erpweb.domain.modules.chatbot.content.projection;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ContentProjection {
    private String cnValue;
    // Users와 JOIN해 얻은 Discord ID
    private String recommenderDiscordId;
}
