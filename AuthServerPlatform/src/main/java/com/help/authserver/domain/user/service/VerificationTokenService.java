package com.help.authserver.domain.user.service;

import java.time.Duration;
import java.util.UUID;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.help.authserver.domain.user.dto.request.SignupRequestDto;
import com.help.authserver.global.common.exception.CustomException;
import com.help.authserver.global.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class VerificationTokenService {
	private static final long EXPIRE_MINUTES = 10L;
	private static final String TOKEN_PREFIX = "auth:email:";
	private static final String PASSWORD_RESET_PREFIX = "auth:pwd:";

	private final RedisTemplate<String, Object> redisTemplate;
	private final ObjectMapper objectMapper;

	public String createToken() {
		return UUID.randomUUID().toString();
	}

	public void saveUserInfoToRedis(final String token, final SignupRequestDto request) {
		try {
			final String userJson = objectMapper.writeValueAsString(request);
			redisTemplate.opsForValue().set(TOKEN_PREFIX + token, userJson, Duration.ofMinutes(EXPIRE_MINUTES));
			log.info("Token: {}, UserRequest: {}", token, userJson);
		} catch (final JsonProcessingException e) {
			log.error("Serialization Error: ", e);
			throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
		}
	}

	public SignupRequestDto getUserInfoFromRedis(final String token) {
		final String userJson = (String)redisTemplate.opsForValue().get(TOKEN_PREFIX + token);
		if (userJson == null) {
			throw new CustomException(ErrorCode.EXPIRED_TOKEN);
		}

		try {
			return objectMapper.readValue(userJson, SignupRequestDto.class);
		} catch (final JsonProcessingException e) {
			log.error("Deserialization Error: ", e);
			throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
		}
	}

	public void saveResetEmailToRedis(final String token, final String email) {
		redisTemplate.opsForValue().set(PASSWORD_RESET_PREFIX + token, email, Duration.ofMinutes(EXPIRE_MINUTES));
	}

	public String getResetEmailFromRedis(final String token) {
		final String email = (String)redisTemplate.opsForValue().get(PASSWORD_RESET_PREFIX + token);
		if (email == null) {
			throw new CustomException(ErrorCode.EXPIRED_TOKEN);
		}
		return email;
	}
}
