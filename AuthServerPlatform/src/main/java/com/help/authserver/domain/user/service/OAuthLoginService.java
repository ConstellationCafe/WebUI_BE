package com.help.authserver.domain.user.service;

import jakarta.servlet.http.HttpServletResponse;

/**
 * OAuth 프로바이더별 로그인 진입점 계약.
 *
 * 프로바이더(Discord, 추후 KakaoTalk 등)마다 code 교환 방식과 프로필 조회
 * 방식은 다르지만, 결과적으로 "코드 하나 받아서 로그인 처리하고 리다이렉트할
 * URL을 돌려준다"는 계약은 동일하다. JWT 발급/Redis 세션 저장 같은
 * 프로바이더 무관 로직은 여기 구현체가 아니라 {@link AuthSessionService}가
 * 담당하며, 구현체는 프로바이더 API 호출 + 로컬 identity 매핑까지만 하고
 * 마지막에 AuthSessionService에 위임한다.
 */
public interface OAuthLoginService {

    String login(String code, HttpServletResponse response);
}
