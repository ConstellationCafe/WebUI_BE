package com.help.erpweb.domain.notification.entity;

/**
 * 알림을 발행한 경로. 멱등 키는 (botId, source, sourceRef, requestKey) 단위로 구분한다.
 * <ul>
 *     <li>ADMIN: 관리자 화면. sourceRef는 발행한 관리자의 Discord ID</li>
 *     <li>INTERNAL: WebUI_BE 내부 기능의 함수 호출. sourceRef는 기능 이름(예: point)</li>
 *     <li>EXTERNAL: API Key로 인증한 외부 시스템. sourceRef는 client ID</li>
 * </ul>
 */
public enum NotificationSource {
    ADMIN,
    INTERNAL,
    EXTERNAL
}
