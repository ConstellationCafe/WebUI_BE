package com.help.global.guild;

/**
 * 요청 1건 동안 유효한 길드(채팅방) 스코프 컨텍스트.
 * <p>
 * {@code X-Guild-Id} 헤더로 들어온 guildId는 {@link GuildContextInterceptor}에서
 * botId로 변환되어 이곳에 보관된다. 이후 서비스/리포지토리 계층은 이 값을 통해
 * "현재 요청이 어느 봇(=어느 채팅방)에 스코프되어 있는지"를 알 수 있다.
 * <p>
 * ADR-0001 참고: guildId와 botId는 1:1이며, 애플리케이션 내부 전파와
 * DB 스코프 단위는 botId로 통일한다.
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
     * @throws IllegalStateException GuildContextInterceptor를 거치지 않은 요청에서 호출된 경우
     */
    public static String requireBotId() {
        final String botId = CURRENT_BOT_ID.get();
        if (botId == null) {
            throw new IllegalStateException(
                "GuildContext가 초기화되지 않았습니다. " +
                    "이 요청 경로가 GuildContextInterceptor를 거치는지 확인하세요."
            );
        }
        return botId;
    }

    public static void clear() {
        CURRENT_BOT_ID.remove();
    }
}
