package com.help.global.chat;

import com.help.global.data.MembershipID;

/**
 * 플랫폼(Discord, KakaoTalk 등)에 종속되지 않는 "채팅 사용자" 도메인 모델.
 * <p>
 * 지금까지 서비스 계층 전반이 discordId를 원시 String으로 주고받으며
 * discord에 지나치게 의존했다. 이 인터페이스는 그 의존을 걷어내기 위한
 * BE 내부 도메인 모델일 뿐이다 — DB 스키마, JWT claim, API 응답 형식은
 * 그대로 둔다. 실제 구현체는 지금은 {@link DiscordChatUser} 하나뿐이고,
 * 두 번째 OAuth 제공자(예: 카카오톡)가 추가되면 새 구현체만 추가하면 되며
 * 이 인터페이스를 참조하는 서비스/레포지토리 코드는 바뀔 필요가 없다.
 */
public interface ChatUser {

    /**
     * 이 사용자가 어느 플랫폼으로 로그인했는지. {@code search_sk_by_bot}의
     * cardType 파라미터와 동일한 값을 쓰는 {@link MembershipID}를 그대로
     * 재사용한다(이미 discord/kakaotalk 두 값을 갖고 있음).
     */
    MembershipID getPlatform();

    /**
     * 플랫폼별 원시 ID. discord면 discordID, kakaotalk이면 kakaotalkID.
     */
    String getExternalId();

    /**
     * 화면 표시용 이름. 알 수 없으면 {@link #getExternalId()}로 대체한다.
     */
    String getDisplayName();
}
