package com.help.authserver.api;

import com.help.authserver.domain.user.dto.response.DiscordGuildResponseDto;
import com.help.authserver.domain.user.dto.response.DiscordMeResponseDto;
import com.help.authserver.domain.user.dto.discord.DiscordGuildDto;
import com.help.authserver.domain.user.dto.discord.DiscordMeDto;
import com.help.authserver.domain.user.dto.user.DiscordUserDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    @Value("${discord.guilds-uri}")
    private String guildsUri;

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
        DiscordMeDto me = this.getMeDto(accessTokenForDiscord);
        List<DiscordGuildDto> guilds = this.getGuildsDto(accessTokenForDiscord);
        return new DiscordUserDto(
                me.id(),
                me.username(),
                me.globalName(),
                me.avatar(),
                guilds
        );
    }

    private DiscordMeDto getMeDto(String accessTokenForDiscord) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessTokenForDiscord);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<DiscordMeResponseDto> meResponse =
                restTemplate.exchange(
                        userUri,
                        HttpMethod.GET,
                        request,
                        DiscordMeResponseDto.class
                );

        DiscordMeResponseDto me = Optional.ofNullable(meResponse.getBody())
                .orElseThrow(() -> new RuntimeException("Discord user response is empty"));

        String avatar;
        if (me.avatar() == null) {
            avatar = "https://cdn.discordapp.com/embed/avatars/0.png";
        } else {
            String extension = me.avatar().startsWith("a_") ? "gif" : "png";
            avatar = "https://cdn.discordapp.com/avatars/"
                    + me.id()
                    + "/"
                    + me.avatar()
                    + "."
                    + extension;
        }
        return new DiscordMeDto(
                me.id(),
                me.username(),
                me.globalName(),
                avatar
        );
    }

    private List<DiscordGuildDto> getGuildsDto(String accessTokenForDiscord) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessTokenForDiscord);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<List<DiscordGuildResponseDto>> guildsResponse =
                restTemplate.exchange(
                        guildsUri,
                        HttpMethod.GET,
                        request,
                        new ParameterizedTypeReference<>() {}
                );

        return Optional.ofNullable(guildsResponse.getBody())
                .orElse(List.of())
                .stream()
                .map(guild -> {
                    String icon = guild.icon();
                    if (icon != null) {
                        String extension = icon.startsWith("a_") ? "gif" : "png";
                        icon = "https://cdn.discordapp.com/icons/"
                                + guild.id()
                                + "/"
                                + icon
                                + "."
                                + extension;
                    }
                    return new DiscordGuildDto(
                            guild.id(),
                            guild.name(),
                            icon
                    );
                })
                .toList();
    }
}
