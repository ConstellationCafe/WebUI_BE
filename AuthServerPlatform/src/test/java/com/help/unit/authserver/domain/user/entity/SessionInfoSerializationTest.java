package com.help.authserver.domain.user.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

/**
 * Redis에 저장되는 세션의 직렬화 호환성(ADR-0008).
 */
class SessionInfoSerializationTest {

    private final GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();

    @Test
    void roundTripsWithDiscordIdField() {
        SessionInfo session = new SessionInfo("123", true, "discord-token", 10L);

        String json = new String(serializer.serialize(session), StandardCharsets.UTF_8);
        SessionInfo restored = (SessionInfo) serializer.deserialize(json.getBytes(StandardCharsets.UTF_8));

        assertThat(json).contains("\"discordId\":\"123\"").doesNotContain("\"username\"");
        assertThat(restored.getDiscordId()).isEqualTo("123");
        assertThat(restored.isValid()).isTrue();
    }

    @Test
    void readsSessionStoredBeforeRename() {
        String legacyJson = "{\"@class\":\"com.help.authserver.domain.user.entity.SessionInfo\","
                + "\"username\":\"123\",\"valid\":true,\"discordAccessToken\":\"discord-token\",\"issuedAt\":10}";

        SessionInfo restored = (SessionInfo) serializer.deserialize(legacyJson.getBytes(StandardCharsets.UTF_8));

        assertThat(restored.getDiscordId()).isEqualTo("123");
        assertThat(restored.getDiscordAccessToken()).isEqualTo("discord-token");
    }
}
