package com.help.erpweb.domain.notification.service;

import com.help.erpweb.domain.notification.entity.NotificationCategory;
import com.help.erpweb.domain.notification.entity.NotificationSource;
import com.help.erpweb.domain.notification.entity.NotificationTargetType;
import com.help.erpweb.domain.notification.exception.InvalidNotificationException;
import java.util.regex.Pattern;

/**
 * 알림 발행 명령. 관리자 API, 외부 API, 내부 함수 호출이 모두 이 타입으로 들어오므로
 * 알림 불변식은 transport DTO가 아니라 여기서 검증한다. 생성 시 문자열을 정규화(trim)한다.
 *
 * @param requestKey 재전송을 같은 알림으로 묶는 멱등 키. 내부 호출처럼 재전송이 없으면 null
 */
public record NotificationCommand(
        String botId,
        NotificationTargetType targetType,
        String targetDiscordId,
        NotificationCategory category,
        String title,
        String body,
        String link,
        NotificationSource source,
        String sourceRef,
        String requestKey
) {
    public static final int TITLE_MAX = 100;
    public static final int BODY_MAX = 1000;
    public static final int LINK_MAX = 255;
    public static final int SOURCE_REF_MAX = 64;
    public static final int REQUEST_KEY_MAX = 64;
    /** config DB bots.bot_id, Notification.bot_id와 같은 VARCHAR(30). 형식은 제한하지 않는다. */
    public static final int BOT_ID_MAX = 30;

    private static final Pattern DISCORD_ID = Pattern.compile("[0-9]{1,20}");
    private static final Pattern REQUEST_KEY = Pattern.compile("[A-Za-z0-9._:-]{1,64}");
    // 앱 내부 경로만 허용한다. 외부 URL과 프로토콜 상대 경로(//host)는 open redirect가 된다.
    private static final Pattern LINK = Pattern.compile("/(?!/)[A-Za-z0-9\\-._~/?=&%]*");

    public NotificationCommand {
        botId = requireWithin(botId, "botId", BOT_ID_MAX);
        if (targetType == null) {
            throw new InvalidNotificationException("알림 대상 유형을 지정하세요.");
        }
        targetDiscordId = blankToNull(targetDiscordId);
        if (targetType == NotificationTargetType.USER) {
            if (targetDiscordId == null || !DISCORD_ID.matcher(targetDiscordId).matches()) {
                throw new InvalidNotificationException("개인 알림은 올바른 대상 Discord ID가 필요합니다.");
            }
        } else if (targetDiscordId != null) {
            throw new InvalidNotificationException("채팅방 전체 알림에는 대상 Discord ID를 지정하지 않습니다.");
        }
        if (category == null) {
            throw new InvalidNotificationException("알림 분류를 지정하세요.");
        }
        title = requireWithin(title, "제목", TITLE_MAX);
        body = requireWithin(body, "내용", BODY_MAX);
        link = blankToNull(link);
        if (link != null && (link.length() > LINK_MAX || !LINK.matcher(link).matches())) {
            throw new InvalidNotificationException("링크는 '/'로 시작하는 앱 내부 경로만 사용할 수 있습니다.");
        }
        if (source == null) {
            throw new InvalidNotificationException("발행 경로를 지정하세요.");
        }
        sourceRef = requireWithin(sourceRef, "발행자", SOURCE_REF_MAX);
        requestKey = blankToNull(requestKey);
        if (requestKey != null && !REQUEST_KEY.matcher(requestKey).matches()) {
            throw new InvalidNotificationException("요청 ID는 64자 이하의 영문, 숫자, '.', '_', ':', '-'만 사용할 수 있습니다.");
        }
    }

    /** 내부 기능이 함수 호출로 발행할 때 쓰는 간단한 생성 경로. */
    public static NotificationCommand internal(
            String botId,
            NotificationTargetType targetType,
            String targetDiscordId,
            NotificationCategory category,
            String title,
            String body,
            String link,
            String feature
    ) {
        return new NotificationCommand(
                botId,
                targetType,
                targetDiscordId,
                category,
                title,
                body,
                link,
                NotificationSource.INTERNAL,
                feature,
                null
        );
    }

    private static String requireWithin(String value, String name, int max) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            throw new InvalidNotificationException(name + "을(를) 입력하세요.");
        }
        if (normalized.length() > max) {
            throw new InvalidNotificationException(name + "은(는) " + max + "자 이하로 입력하세요.");
        }
        return normalized;
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
