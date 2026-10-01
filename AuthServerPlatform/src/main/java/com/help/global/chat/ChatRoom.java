package com.help.global.chat;

import com.help.global.data.MembershipID;

/**
 * 플랫폼에 종속되지 않는 "채팅방" 도메인 모델.
 * <p>
 * Discord에서는 guildId와 1:1인 botId를 내부 room id로 쓴다 —
 * {@link com.help.global.guild.GuildContext}가 이미 스코프 단위로 쓰고
 * 있는 값과 같다. roomName은 로컬 DB(erp_subscriber 등)에 저장돼 있지
 * 않으므로 알 수 없으면 null일 수 있다; 화면에 방 이름이 필요하면
 * 로그인/방 목록 조회 시점에만 존재하는 DiscordGuildDto를 쓴다.
 * DB/JWT/API는 바꾸지 않는다 — BE 내부 도메인 모델일 뿐이다.
 */
public interface ChatRoom {

    MembershipID getPlatform();

    /**
     * 내부 스코프 키. discord면 botId(guildId와 1:1).
     */
    String getRoomId();

    /**
     * 표시용 방 이름. 로컬에 없으면 null일 수 있다.
     */
    String getRoomName();
}
