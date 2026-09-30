package com.help.erpweb.domain.modules.competition.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.help.erpweb.domain.config.entity.BotEnv;
import com.help.erpweb.domain.config.entity.ModuleConfig;
import com.help.erpweb.domain.config.repository.BotEnvRepository;
import com.help.erpweb.domain.config.repository.ModuleConfigRepository;
import com.help.erpweb.domain.modules.competition.client.DiscordBotClient;
import com.help.erpweb.domain.modules.competition.dto.response.CompetitionBoardResponse;
import com.help.erpweb.domain.modules.competition.dto.response.CompetitionPostResponse;
import com.help.erpweb.domain.modules.competition.dto.response.CompetitionPreviewResponse;
import com.help.erpweb.domain.modules.competition.exception.CompetitionConfigNotFoundException;
import com.help.erpweb.domain.modules.competition.exception.InvalidCompetitionNoticeException;
import com.help.global.guild.GuildContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 대회 공지를 현재 채팅방(botId)의 대회 게시판에 봇 계정으로 게시한다.
 *
 * <p>WebUI_BE는 글만 쓴다. 대회 등록(스케줄러), 참가 이모지·역할·대회방 생성, WebUI 알림 발행은
 * 봇이 게시판 글을 감지해 기존 흐름({@code CompetitionService.host_competition})으로 처리한다.
 * 게시판 목록과 봇 토큰은 config DB({@code module_config}, {@code bot_env})에서 읽기만 한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompetitionService {
	static final String MODULE_ID = "network_operations";
	/** 봇이 참가 이모지·역할·대회방을 만드는 게시판. 봇 정책과 같다. */
	static final String JOINABLE_BOARD = "inner_board";
	static final ZoneId KST = ZoneId.of("Asia/Seoul");
	private static final int NONCE_LENGTH = 25; // Discord nonce 최대 길이

	private final ModuleConfigRepository moduleConfigRepository;
	private final BotEnvRepository botEnvRepository;
	private final DiscordBotClient discordBotClient;
	private final Clock clock;

	public List<CompetitionBoardResponse> boards() {
		final String botId = GuildContext.requireBotId();
		final Map<String, String> boards = loadBoards(loadCompetitionConfig(botId));
		final String token = requireBotEnv(botId).getDiscordToken();
		return boards.entrySet().stream()
			.map(board -> new CompetitionBoardResponse(
				board.getKey(),
				board.getValue(),
				discordBotClient.findChannelName(token, board.getValue()).orElse(board.getKey()),
				JOINABLE_BOARD.equals(board.getKey())))
			.toList();
	}

	public CompetitionPreviewResponse preview(CompetitionNotice notice) {
		return new CompetitionPreviewResponse(CompetitionNoticeFormatter.format(notice, now()));
	}

	public CompetitionPostResponse post(String requestId, String boardKey, CompetitionNotice notice, String adminId) {
		final String botId = GuildContext.requireBotId();
		final String content = CompetitionNoticeFormatter.format(notice, now());

		final JsonNode competitionConfig = loadCompetitionConfig(botId);
		final String channelId = loadBoards(competitionConfig).get(boardKey);
		if (channelId == null) {
			throw new InvalidCompetitionNoticeException("현재 채팅방에 '" + boardKey + "' 게시판이 없습니다.");
		}
		final BotEnv botEnv = requireBotEnv(botId);

		final String messageId = discordBotClient.createMessage(
			botEnv.getDiscordToken(), channelId, content, nonce(botId, requestId));
		log.info("대회 공지 게시 - botId={}, board={}, channelId={}, messageId={}, adminId={}",
			botId, boardKey, channelId, messageId, adminId);

		final String guildId = guildId(competitionConfig, botEnv);
		final String messageUrl = guildId == null
			? null
			: "https://discord.com/channels/" + guildId + "/" + channelId + "/" + messageId;
		return new CompetitionPostResponse(boardKey, channelId, messageId, messageUrl, content);
	}

	/** module_config의 network_operations 설정에서 add_on.competition을 꺼낸다. */
	JsonNode loadCompetitionConfig(String botId) {
		final ModuleConfig moduleConfig = moduleConfigRepository.findByIdBotIdAndIdModuleId(botId, MODULE_ID)
			.orElseThrow(() -> new CompetitionConfigNotFoundException("현재 채팅방에 network_operations 설정이 없습니다."));
		JsonNode root = moduleConfig.getConfig();
		if (root == null) {
			throw new CompetitionConfigNotFoundException("현재 채팅방에 network_operations 설정이 없습니다.");
		}
		// 설정을 manifest처럼 {"config": {...}}로 감싼 경우와 바로 저장한 경우를 모두 지원한다.
		if (root.has("config") && root.get("config").isObject()) {
			root = root.get("config");
		}
		final JsonNode competition = root.path("add_on").path("competition");
		if (!competition.isObject()) {
			throw new CompetitionConfigNotFoundException("현재 채팅방에 대회(competition) 설정이 없습니다.");
		}
		return competition;
	}

	/** notice_channel.boards를 {키: 채널 ID}로 읽는다. 숫자가 아닌 값은 건너뛴다. */
	static Map<String, String> loadBoards(JsonNode competitionConfig) {
		final Map<String, String> boards = new LinkedHashMap<>();
		competitionConfig.path("notice_channel").path("boards").fields().forEachRemaining(board -> {
			final String channelId = board.getValue().asText("");
			if (board.getValue().isValueNode() && channelId.matches("[0-9]{1,20}")) {
				boards.put(board.getKey(), channelId);
			} else {
				log.warn("대회 게시판 설정 값이 채널 ID가 아니라 건너뜁니다 - board={}", board.getKey());
			}
		});
		if (boards.isEmpty()) {
			throw new CompetitionConfigNotFoundException("현재 채팅방에 대회 게시판 설정이 없습니다.");
		}
		return boards;
	}

	private BotEnv requireBotEnv(String botId) {
		final BotEnv botEnv = botEnvRepository.findById(botId)
			.orElseThrow(() -> new CompetitionConfigNotFoundException("현재 채팅방의 봇 실행 환경이 없습니다."));
		if (botEnv.getDiscordToken() == null || botEnv.getDiscordToken().isBlank()) {
			throw new CompetitionConfigNotFoundException("현재 채팅방의 봇 토큰이 설정되지 않았습니다.");
		}
		return botEnv;
	}

	private static String guildId(JsonNode competitionConfig, BotEnv botEnv) {
		final String configured = competitionConfig.path("guild_id").asText("");
		if (configured.matches("[0-9]{1,20}")) {
			return configured;
		}
		return botEnv.getGuildId() == null ? null : String.valueOf(botEnv.getGuildId());
	}

	/** 같은 채팅방의 같은 요청 ID는 같은 nonce가 되어, 디스코드가 짧은 시간 안의 재시도를 한 번만 게시한다. */
	static String nonce(String botId, String requestId) {
		try {
			final byte[] digest = MessageDigest.getInstance("SHA-256")
				.digest((botId + ":" + requestId).getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest).substring(0, NONCE_LENGTH);
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", ex);
		}
	}

	private LocalDateTime now() {
		return LocalDateTime.ofInstant(clock.instant(), KST);
	}
}
