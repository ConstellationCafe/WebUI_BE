package com.help.global.chat;

import com.help.global.guild.GuildContext;
import com.help.global.jwt.CustomUser;

/**
 * {@link CustomUser}(Spring Security principal)와
 * {@link GuildContext}(현재 요청의 botId)로부터 {@link ChatUser}/{@link ChatRoom}을
 * 만들어내는 유일한 지점.
 * <p>
 * 지금은 로그인 경로가 Discord 하나뿐이라 항상 {@link DiscordChatUser}/
 * {@link DiscordChatRoom}을 반환한다. 두 번째 OAuth 제공자(카카오톡 등)의
 * 로그인 흐름이 실제로 생기면 이 클래스에만 분기를 추가하면 되고,
 * 호출부(서비스 계층)는 {@link ChatUser}/{@link ChatRoom} 인터페이스만
 * 바라보므로 바뀔 필요가 없다.
 */
public final class ChatIdentities {

    private ChatIdentities() {
    }

    /**
     * 현재 요청의 인증 principal로부터 ChatUser를 만든다.
     */
    public static ChatUser fromPrincipal(final CustomUser user) {
        return new DiscordChatUser(user.getUsername(), user.getNickname());
    }

    /**
     * 이미 discordId만 알고 있는 경우(예: DTO에 담겨 온 다른 사람의 discordId)
     * 쓰는 생성 경로. principal이 없을 때만 사용한다.
     */
    public static ChatUser ofDiscord(final String discordId) {
        return new DiscordChatUser(discordId, null);
    }

    /**
     * 현재 요청에 스코프된(=로그인 2단계에서 검증된) 채팅방.
     *
     * @throws IllegalStateException GuildContext.requireBotId()와 동일한 조건에서 발생
     */
    public static ChatRoom currentRoom() {
        return new DiscordChatRoom(GuildContext.requireBotId(), null);
    }
}
