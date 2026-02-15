package com.help.authserver.api;

import com.help.authserver.domain.user.dto.user.DiscordUserDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class DiscordAPI implements LoginAPI<DiscordUserDto> {
    @Value("${discord.client-id}")
    private String clientId;

    @Value("${discord.client-secret}")
    private String clientSecret;

    @Value("${discord.token-uri}")
    private String tokenUri;

    @Value("${discord.user-uri}")
    private String userUri;
    @Value("${discord.redirect-uri}")
    private String redirectUri;

    private final RestTemplate restTemplate = new RestTemplate();

    public String exchangeCodeForToken(String code) {
        // Content-Type 헤더를 application/x-www-form-urlencoded 로 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // OAuth2 토큰 발급 요청
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri", redirectUri);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            tokenUri,
            HttpMethod.POST,
            new HttpEntity<>(body, headers),
            new ParameterizedTypeReference<>(){}
        );

        // access_token 추출 & 반환
        if (response.getBody().get("access_token") != null) {
            return (String) response.getBody().get("access_token");
        } else {
            throw new RuntimeException("Failed to retrieve access token from Discord");
        }
    }

    @Override
    public DiscordUserDto getUserInfo(String accessTokenForDiscord) {
        // 발급받은 accessToken으로 사용자 정보 조회 -> 회원가입
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessTokenForDiscord);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(userUri, HttpMethod.GET, request, Map.class);

        Map body = response.getBody();
        String discordId = (String) body.get("id");
        String username = (String) body.get("username");
        String avatar = (String) body.get("avatar");

        return new DiscordUserDto(discordId, username, avatar);
    }
}
