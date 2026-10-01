package com.help.global.guild;

/**
 * 요청 1건 동안 유효한 길드(채팅방) 스코프 컨텍스트.
 * <p>
 * ADR-0001: 로그인은 discordId 인증만으로는 완료되지 않고, {@code /auth/guild/select}로
 * 채팅방(guildId)을 선택해야 완료된다. 선택 시점에 botId가 AccessToken/RefreshToken
 * claim으로 발급되고, 이후 모든 {@code /api/**} 요청에서 {@link com.help.global.jwt.BackEndJwtAuthFilter}가
 * 그 토큰에서 botId를 꺼내 이곳에 저장한다. 서비스/리포지토리 계층은 이 값을 통해
 * "현재 요청이 어느 봇(=어느 채팅방)에 스코프되어 있는지"를 알 수 있다.
 * <p>
 * guildId와 botId는 1:1이며, 애플리케이션 내부 전파와 DB 스코프 단위는
 * botId로 통일한다.
 */
public final class GuildContext {
    private static final ThreadLocal<String> CURRENT_BOT_ID = new ThreadLocal<>();

    private GuildContext() {
    }

    public static void setBotId(final String botId) {
        CURRENT_BOT_ID.set(botId);
    }

    /**
     * 현재 요청에 스코프된 botId를 반환한다.
     *
     * @throws IllegalStateException botId claim이 없는 토큰(로그인 미완료) 또는
     *         BackEndJwtAuthFilter를 거치지 않은 요청에서 호출된 경우
     */
    public static String requireBotId() {
        final String botId = CURRENT_BOT_ID.get();
        if (botId == null) {
            throw new IllegalStateException(
                "GuildContext가 초기화되지 않았습니다. " +
                    "이 요청 경로가 BackEndJwtAuthFilter를 거치는지, " +
                    "채팅방 선택(로그인 완료)이 끝났는지 확인하세요."
            );
        }
        return botId;
    }

    public static void clear() {
        CURRENT_BOT_ID.remove();
    }
}
