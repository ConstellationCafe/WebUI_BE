package com.help.backend.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

// @Configuration
// public class RedisConfig {
// 	@Value("${spring.data.redis.host}")
// 	private String redisHost;
//
// 	@Value("${spring.data.redis.port}")
// 	private int redisPort;
//
// 	@Bean
// 	public RedisConnectionFactory redisConnectionFactory() {
// 		final RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
// 		return new LettuceConnectionFactory(config);
// 	}
//
// 	@Bean
// 	public RedisTemplate<String, Object> redisTemplate(final RedisConnectionFactory redisConnectionFactory) {
// 		final RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
//
// 		redisTemplate.setConnectionFactory(redisConnectionFactory);
//
// 		redisTemplate.setKeySerializer(new StringRedisSerializer());
// 		redisTemplate.setHashKeySerializer(new StringRedisSerializer());
//
// 		redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
// 		redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
//
// 		return redisTemplate;
// 	}
// }
@Configuration
public class RedisConfig {

	@Bean
	public RedisTemplate<String, Object> redisTemplate(final RedisConnectionFactory redisConnectionFactory) {
		final RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();

		redisTemplate.setConnectionFactory(redisConnectionFactory);
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setHashKeySerializer(new StringRedisSerializer());
		redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
		redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

		return redisTemplate;
	}
}
