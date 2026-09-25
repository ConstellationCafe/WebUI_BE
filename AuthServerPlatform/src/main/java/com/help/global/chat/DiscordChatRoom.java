package com.help.global.chat;

import com.help.global.data.MembershipID;

/**
 * {@link ChatRoom}의 Discord 구현체. botId(guildId와 1:1)를 roomId로 쓴다.
 */
public record DiscordChatRoom(String botId, String roomName) implements ChatRoom {

    @Override
    public MembershipID getPlatform() {
        return MembershipID.discord;
    }

    @Override
    public String getRoomId() {
        return botId;
    }

    @Override
    public String getRoomName() {
        return roomName;
    }
}
