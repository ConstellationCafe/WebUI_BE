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

    private String key(String username) {
        return PREFIX + username;
    }

    public void save(SessionInfo sessionInfo, Duration ttl) {
        redisTemplate.opsForValue().set(
                key(sessionInfo.getUsername()),
                sessionInfo,
                ttl
        );
    }

    public Optional<SessionInfo> find(String username) {
        Object value = redisTemplate.opsForValue().get(key(username));
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of((SessionInfo) value);
    }

    public void invalidate(String username) {
        find(username).ifPresent(session -> {
            SessionInfo revoked = new SessionInfo(
                    session.getUsername(),
                    false,
                    session.getDiscordAccessToken(),
                    session.getIssuedAt()
            );
            String key = key(username);
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            redisTemplate.opsForValue().set(key, revoked);
            if (ttl != null && ttl > 0) {
                redisTemplate.expire(key, ttl, TimeUnit.SECONDS);
            }
        });
    }

    public void refreshTtl(String username, Duration ttl) {
        String key = key(username);

        Boolean exists = redisTemplate.hasKey(key);
        if (Boolean.FALSE.equals(exists)) {
            throw new CustomException(ErrorCode.SESSION_EXPIRED);
        }
        redisTemplate.expire(key, ttl);
    }

    public void delete(String username) {
        redisTemplate.delete(key(username));
    }
}