package com.help.erpweb.domain.modules.chatbot.menu.projection;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MenuProjection {
    private String mnValue;
    // Users와 JOIN해 얻은 Discord ID
    private String recommenderDiscordId;
}
