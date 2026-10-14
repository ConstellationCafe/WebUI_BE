package com.help.authserver.domain.user.repository;

import com.help.authserver.domain.user.entity.SessionInfo;
import com.help.global.common.exception.CustomException;
import com.help.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class SessionRepository {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String PREFIX = "session:user:";

    private String key(String discordId) {
        return PREFIX + discordId;
    }

    public void save(SessionInfo sessionInfo, Duration ttl) {
        redisTemplate.opsForValue().set(
                key(sessionInfo.getDiscordId()),
                sessionInfo,
                ttl
        );
    }

    public Optional<SessionInfo> find(String discordId) {
        Object value = redisTemplate.opsForValue().get(key(discordId));
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of((SessionInfo) value);
    }

    public void invalidate(String discordId) {
        find(discordId).ifPresent(session -> {
            SessionInfo revoked = new SessionInfo(
                    session.getDiscordId(),
                    false,
                    session.getDiscordAccessToken(),
                    session.getIssuedAt()
            );
            String key = key(discordId);
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            redisTemplate.opsForValue().set(key, revoked);
            if (ttl != null && ttl > 0) {
                redisTemplate.expire(key, ttl, TimeUnit.SECONDS);
            }
        });
    }

    public void refreshTtl(String discordId, Duration ttl) {
        String key = key(discordId);

        Boolean exists = redisTemplate.hasKey(key);
        if (Boolean.FALSE.equals(exists)) {
            throw new CustomException(ErrorCode.SESSION_EXPIRED);
        }
        redisTemplate.expire(key, ttl);
    }

    public void delete(String discordId) {
        redisTemplate.delete(key(discordId));
    }
}
