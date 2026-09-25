package com.help.global.chat;

import com.help.global.data.MembershipID;

/**
 * {@link ChatUser}의 Discord 구현체. discordId를 externalId로 그대로 쓴다.
 */
public record DiscordChatUser(String discordId, String nickname) implements ChatUser {

    @Override
    public MembershipID getPlatform() {
        return MembershipID.discord;
    }

    @Override
    public String getExternalId() {
        return discordId;
    }

    @Override
    public String getDisplayName() {
        return (nickname == null || nickname.isBlank()) ? discordId : nickname;
    }
}
