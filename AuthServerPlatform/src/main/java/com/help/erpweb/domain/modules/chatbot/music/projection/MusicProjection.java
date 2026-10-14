package com.help.erpweb.domain.modules.chatbot.music.projection;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MusicProjection {
    private String videoId;
    // Users와 JOIN해 얻은 Discord ID
    private String recommenderDiscordId;
}
