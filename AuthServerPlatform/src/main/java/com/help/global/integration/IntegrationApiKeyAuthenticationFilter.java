package com.help.global.integration;

import com.help.global.data.Authority;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * {@value #HEADER} 헤더의 API Key를 SHA-256으로 해시해 설정된 client와 비교한다.
 * <p>
 * 일치하면 {@link Authority#INTEGRATION} 권한으로 인증하고, 아니면 인증 없이 통과시켜
 * 인가 단계에서 401이 나가게 한다. 비교는 상수 시간 비교를 쓰고 첫 일치에서 멈추지 않는다.
 * 원본 키와 해시는 로그에 남기지 않는다. 이 필터는 Spring bean으로 등록하지 않는다
 * (서블릿 전역 필터로 자동 등록되지 않게 integration chain에서만 생성한다).
 */
@Slf4j
public class IntegrationApiKeyAuthenticationFilter extends OncePerRequestFilter {
    public static final String HEADER = "X-Api-Key";

    private final List<ResolvedClient> clients;

    public IntegrationApiKeyAuthenticationFilter(IntegrationClientProperties properties) {
        HexFormat hex = HexFormat.of();
        this.clients = properties.clients().stream()
                .map(client -> new ResolvedClient(
                        new IntegrationClient(client.id(), client.botIdSet()),
                        hex.parseHex(client.keySha256())
                ))
                .toList();
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String apiKey = request.getHeader(HEADER);
        if (apiKey != null && !apiKey.isBlank()) {
            IntegrationClient matched = match(apiKey.trim());
            if (matched != null) {
                UsernamePasswordAuthenticationToken authentication =
                        UsernamePasswordAuthenticationToken.authenticated(
                                matched,
                                null,
                                List.of(new SimpleGrantedAuthority(Authority.INTEGRATION))
                        );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                log.warn("등록되지 않은 integration API Key 요청 - uri={}", request.getRequestURI());
            }
        }
        filterChain.doFilter(request, response);
    }

    IntegrationClient match(String apiKey) {
        byte[] digest = sha256(apiKey);
        IntegrationClient matched = null;
        for (ResolvedClient client : clients) {
            if (MessageDigest.isEqual(digest, client.keyHash()) && matched == null) {
                matched = client.client();
            }
        }
        return matched;
    }

    public static String sha256Hex(String value) {
        return HexFormat.of().formatHex(sha256(value));
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", ex);
        }
    }

    private record ResolvedClient(IntegrationClient client, byte[] keyHash) {
    }
}
