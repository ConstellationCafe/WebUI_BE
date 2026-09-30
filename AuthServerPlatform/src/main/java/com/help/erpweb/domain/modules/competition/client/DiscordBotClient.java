package com.help.erpweb.domain.modules.competition.client;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.help.erpweb.domain.modules.competition.exception.CompetitionPostFailedException;

import lombok.extern.slf4j.Slf4j;

/**
 * 봇 토큰으로 Discord REST API를 호출한다. OAuth 사용자 토큰을 쓰는 {@code DiscordAPI}와 분리한다.
 * 봇 토큰과 디스코드 응답 본문은 로그와 예외 메시지에 남기지 않는다.
 */
@Slf4j
@Component
public class DiscordBotClient {
	private static final ParameterizedTypeReference<Map<String, Object>> JSON_OBJECT =
		new ParameterizedTypeReference<>() {
		};

	private final RestTemplate restTemplate;
	private final String apiBaseUri;

	public DiscordBotClient(
		RestTemplate restTemplate,
		@Value("${discord.bot-api-uri:https://discord.com/api/v10}") String apiBaseUri
	) {
		this.restTemplate = restTemplate;
		this.apiBaseUri = apiBaseUri.endsWith("/") ? apiBaseUri.substring(0, apiBaseUri.length() - 1) : apiBaseUri;
	}

	/** 채널 이름을 조회한다. 실패하면 빈 값을 돌려주고, 호출자는 설정 키로 대신 표시한다. */
	public Optional<String> findChannelName(String botToken, String channelId) {
		try {
			final ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
				apiBaseUri + "/channels/" + channelId,
				HttpMethod.GET,
				new HttpEntity<>(headers(botToken)),
				JSON_OBJECT);
			return Optional.ofNullable(response.getBody())
				.map(body -> body.get("name"))
				.filter(String.class::isInstance)
				.map(String.class::cast);
		} catch (HttpStatusCodeException ex) {
			log.warn("디스코드 채널 조회 실패 - channelId={}, status={}", channelId, ex.getStatusCode().value());
		} catch (RestClientException ex) {
			log.warn("디스코드 채널 조회 실패 - channelId={}, reason={}", channelId, ex.getClass().getSimpleName());
		}
		return Optional.empty();
	}

	/**
	 * 채널에 메시지를 게시하고 메시지 ID를 돌려준다.
	 * 멘션은 모두 비활성화하고, nonce를 강제해 짧은 시간 안의 재시도가 같은 메시지로 처리되게 한다.
	 */
	public String createMessage(String botToken, String channelId, String content, String nonce) {
		final Map<String, Object> body = new LinkedHashMap<>();
		body.put("content", content);
		body.put("allowed_mentions", Map.of("parse", List.of()));
		body.put("nonce", nonce);
		body.put("enforce_nonce", true);
		try {
			final ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
				apiBaseUri + "/channels/" + channelId + "/messages",
				HttpMethod.POST,
				new HttpEntity<>(body, headers(botToken)),
				JSON_OBJECT);
			final Object messageId = Optional.ofNullable(response.getBody())
				.map(responseBody -> responseBody.get("id"))
				.orElse(null);
			if (messageId instanceof String id && !id.isBlank()) {
				return id;
			}
			log.error("디스코드 메시지 게시 응답에 id가 없습니다 - channelId={}", channelId);
			throw new CompetitionPostFailedException("디스코드 응답을 확인하지 못했습니다. 게시판에 글이 올라갔는지 확인해 주세요.");
		} catch (HttpStatusCodeException ex) {
			final int status = ex.getStatusCode().value();
			log.error("디스코드 메시지 게시 실패 - channelId={}, status={}", channelId, status);
			throw new CompetitionPostFailedException(postFailureMessage(status), ex);
		} catch (RestClientException ex) {
			log.error("디스코드 메시지 게시 실패 - channelId={}, reason={}", channelId, ex.getClass().getSimpleName());
			throw new CompetitionPostFailedException(
				"디스코드에 연결하지 못했습니다. 게시판에 글이 올라갔는지 확인한 뒤 다시 시도해 주세요.", ex);
		}
	}

	private static String postFailureMessage(int status) {
		if (status == 401) {
			return "봇 토큰이 유효하지 않아 공지를 게시하지 못했습니다. 봇 설정을 확인해 주세요.";
		}
		if (status == 403) {
			return "봇에게 게시판 글쓰기 권한이 없습니다. 디스코드 채널 권한을 확인해 주세요.";
		}
		if (status == 404) {
			return "게시판 채널을 찾을 수 없습니다. 대회 게시판 설정을 확인해 주세요.";
		}
		if (status == 429) {
			return "디스코드 요청이 많아 잠시 후 다시 시도해 주세요.";
		}
		return "디스코드에 공지를 게시하지 못했습니다. 잠시 후 다시 시도해 주세요.";
	}

	private static HttpHeaders headers(String botToken) {
		final HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.AUTHORIZATION, "Bot " + botToken);
		headers.setContentType(MediaType.APPLICATION_JSON);
		return headers;
	}
}
