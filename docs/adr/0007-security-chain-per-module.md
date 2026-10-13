# ADR-0007: 보안 체인은 경로를 소유한 모듈이 가지고, global은 모듈에 의존하지 않는다

- 상태: Accepted
- 날짜: 2026-10-12
- 적용 범위: WebUI_BE Security 설정 (`/auth/**`, `/api/**` 체인과 JWT 필터 위치)
- 담당자: 미정 (프로젝트 문서 담당자 지정 필요)
- 관련: [ADR-0001](0001-guild-scope.md), [ADR-0002](0002-admin-api-prefix.md), [아키텍처](../architecture.md)

## Context

authserver(`/auth/**`)와 erpweb(`/api/**`)는 이후 서비스 분리를 전제로 패키지를 나눴다. 이 준비로 `AuthServerJwtAuthFilter`는 authserver의 로그인 세션에 의존하므로 `global.jwt`에서 `authserver.security`로 옮겨졌다. 그러나 정리가 중간에 멈춰 다음 비대칭이 남았다.

- `/api/**` 전용 필터 `BackEndJwtAuthFilter`는 `GuildContext`에 `botId`를 넣는 erpweb 인증인데 `global.jwt`에 있었다.
- 두 체인을 만드는 `global.config.security.SecurityConfig`가 `authserver.security`를 import했다. 프로젝트에서 global이 authserver에 의존하는 유일한 지점이었고, 필터 이동으로 없애려던 의존이 설정 클래스에 그대로 남았다.

외부 연동 체인은 이미 `global.integration.IntegrationSecurityConfig`가 자기 경로의 체인을 직접 소유하는 방식이었다.

## Decision

1. 경로별 `SecurityFilterChain`은 그 경로를 소유한 모듈의 `security` 패키지에 둔다.
   - `@Order(0)` `/api/integrations/**` — `global.integration.IntegrationSecurityConfig` (변경 없음)
   - `@Order(1)` `/auth/**` — `authserver.security.AuthServerSecurityConfig`
   - `@Order(2)` `/api/**` — `erpweb.security.ErpWebSecurityConfig`
   - `@Order(3)` `/actuator/**` — `global.config.security.SecurityConfig`
2. 각 체인의 JWT 필터도 같은 모듈에 둔다. `BackEndJwtAuthFilter`는 `erpweb.security`로 옮긴다.
3. `global.config.security`에는 모듈 공용 부품만 둔다: CORS 설정(bean 이름 `SecurityConfig.CORS_CONFIGURATION_SOURCE`), `PasswordEncoder`, `JsonAuthenticationEntryPoint`·`JsonAccessDeniedHandler`, `@EnableWebSecurity`·`@EnableMethodSecurity`, actuator 체인.
4. 양쪽이 같이 쓰는 토큰 도구(`JwtUtil`, `CustomUser`)는 `global.jwt`에 남긴다.
5. 의존 방향은 `authserver → global`, `erpweb → global`만 허용한다. global의 보안 설정은 authserver·erpweb을 import하지 않는다.

체인 순서, 매칭 경로, 필터 위치, 인가 규칙, 401/404 응답은 바꾸지 않는다.

## 검토한 대안

- `BackEndJwtAuthFilter`만 `erpweb`으로 이동: 패키지 모양은 대칭이 되지만 global의 `SecurityConfig`가 authserver와 erpweb 둘 다 import하게 되어 의존 방향 문제가 커진다.
- `AuthServerJwtAuthFilter`를 `global.jwt`로 복귀: global → authserver 의존을 다시 만들어 서비스 분리 준비와 반대 방향이다.
- 체인마다 CORS 설정을 따로 정의: 허용 origin이 같은 설정이 둘로 갈라져 어긋날 위험이 있어, 공용 bean 하나를 이름으로 주입한다.

## 호환성과 운영 영향

API 계약, DB 스키마, 설정 키는 바뀌지 않는다. `/auth/**` 체인의 bean 이름이 `filterChain`에서 `authServerChain`으로 바뀌지만 이름으로 참조하는 곳은 없다. `HandlerMappingIntrospector`도 `CorsConfigurationSource`를 구현하므로 모듈 체인은 CORS bean을 이름(`@Qualifier`)으로 주입한다.

## 남은 과제

- `global.common.exception.GlobalExceptionHandler`가 erpweb 포인트 예외를 import하는 global → erpweb 의존이 남아 있다. 별도 작업으로 정리한다.
