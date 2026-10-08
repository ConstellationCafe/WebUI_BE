package com.help.erpweb.domain.modules.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.help.erpweb.domain.config.entity.BotEnv;
import com.help.erpweb.domain.config.entity.ModuleConfig;
import com.help.erpweb.domain.config.repository.BotEnvRepository;
import com.help.erpweb.domain.config.repository.ModuleConfigRepository;
import com.help.erpweb.domain.modules.competition.client.DiscordBotClient;
import com.help.erpweb.domain.modules.competition.dto.response.CompetitionBoardResponse;
import com.help.erpweb.domain.modules.competition.dto.response.CompetitionPostResponse;
import com.help.erpweb.domain.modules.competition.exception.CompetitionConfigNotFoundException;
import com.help.erpweb.domain.modules.competition.exception.InvalidCompetitionNoticeException;
import com.help.global.guild.GuildContext;

@ExtendWith(MockitoExtension.class)
class CompetitionServiceTest {
	private static final String BOT_ID = "bot-a";
	private static final String TOKEN = "bot-token";
	/** 2026-09-30 12:00 KST */
	private static final Instant NOW = Instant.parse("2026-09-30T03:00:00Z");
	private static final String CONFIG = """
		{"add_on": {"competition": {
			"guild_id": 1529292150565634178,
			"notice_channel": {"boards": {"inner_board": 111, "outer_board": "222", "broken": "외부대회게시판"}}
		}}}""";

	@Mock
	private ModuleConfigRepository moduleConfigRepository;
	@Mock
	private BotEnvRepository botEnvRepository;
	@Mock
	private DiscordBotClient discordBotClient;
	private CompetitionService service;

	@BeforeEach
	void setUp() {
		service = new CompetitionService(moduleConfigRepository, botEnvRepository, discordBotClient,
			Clock.fixed(NOW, ZoneOffset.UTC));
		GuildContext.setBotId(BOT_ID);
	}

	@AfterEach
	void tearDown() {
		GuildContext.clear();
	}

	@Test
	void listsBoardsOfCurrentBotAndMarksOnlyInnerBoardJoinable() throws Exception {
		givenConfig(CONFIG);
		givenBotEnv(TOKEN);
		when(discordBotClient.findChannelName(TOKEN, "111")).thenReturn(Optional.of("내부대회게시판"));
		when(discordBotClient.findChannelName(TOKEN, "222")).thenReturn(Optional.empty());

		List<CompetitionBoardResponse> boards = service.boards();

		assertThat(boards).containsExactly(
			new CompetitionBoardResponse("inner_board", "111", "내부대회게시판", true),
			new CompetitionBoardResponse("outer_board", "222", "outer_board", false));
	}

	@Test
	void readsBoardsWhenConfigIsWrappedLikeManifest() throws Exception {
		givenConfig("{\"config\": " + CONFIG + "}");
		givenBotEnv(TOKEN);
		when(discordBotClient.findChannelName(anyString(), anyString())).thenReturn(Optional.empty());

		assertThat(service.boards()).extracting(CompetitionBoardResponse::key)
			.containsExactly("inner_board", "outer_board");
	}

	@Test
	void postsFormattedNoticeToSelectedBoardAsBot() throws Exception {
		givenConfig(CONFIG);
		givenBotEnv(TOKEN);
		when(discordBotClient.createMessage(eq(TOKEN), eq("222"), anyString(), anyString())).thenReturn("999");

		CompetitionPostResponse response = service.post("req-1", "outer_board", validNotice(), "admin-1");

		assertThat(response.messageId()).isEqualTo("999");
		assertThat(response.messageUrl()).isEqualTo("https://discord.com/channels/1529292150565634178/222/999");
		assertThat(response.content()).startsWith("\"미니미 대회\"가 개최되었습니다 !");
		verify(discordBotClient).createMessage(TOKEN, "222", response.content(),
			CompetitionService.nonce(BOT_ID, "req-1"));
	}

	@Test
	void nonceIsStablePerRequestAndFitsDiscordLimit() {
		assertThat(CompetitionService.nonce(BOT_ID, "req-1"))
			.hasSize(25)
			.isEqualTo(CompetitionService.nonce(BOT_ID, "req-1"))
			.isNotEqualTo(CompetitionService.nonce("bot-b", "req-1"));
	}

	@Test
	void rejectsUnknownBoardWithoutPosting() throws Exception {
		givenConfig(CONFIG);

		assertThatThrownBy(() -> service.post("req-1", "broken", validNotice(), "admin-1"))
			.isInstanceOf(InvalidCompetitionNoticeException.class);
		verify(discordBotClient, never()).createMessage(any(), any(), any(), any());
	}

	@Test
	void rejectsInvalidNoticeBeforeReadingConfig() {
		CompetitionNotice invalid = new CompetitionNotice("a\"b", "x", "y",
			LocalDateTime.of(2026, 10, 1, 0, 0), LocalDateTime.of(2026, 10, 2, 0, 0),
			LocalDateTime.of(2026, 10, 2, 0, 0), List.of(), List.of());

		assertThatThrownBy(() -> service.post("req-1", "inner_board", invalid, "admin-1"))
			.isInstanceOf(InvalidCompetitionNoticeException.class);
		verify(moduleConfigRepository, never()).findByIdBotIdAndIdModuleId(any(), any());
	}

	@Test
	void failsWhenCurrentBotHasNoCompetitionConfigOrToken() throws Exception {
		when(moduleConfigRepository.findByIdBotIdAndIdModuleId(BOT_ID, CompetitionService.MODULE_ID))
			.thenReturn(Optional.empty());
		assertThatThrownBy(() -> service.boards()).isInstanceOf(CompetitionConfigNotFoundException.class);

		givenConfig(CONFIG);
		givenBotEnv(" ");
		assertThatThrownBy(() -> service.boards()).isInstanceOf(CompetitionConfigNotFoundException.class);
	}

	private void givenConfig(String json) throws Exception {
		ModuleConfig moduleConfig = mock(ModuleConfig.class);
		when(moduleConfig.getConfig()).thenReturn(new ObjectMapper().readTree(json));
		when(moduleConfigRepository.findByIdBotIdAndIdModuleId(BOT_ID, CompetitionService.MODULE_ID))
			.thenReturn(Optional.of(moduleConfig));
	}

	private void givenBotEnv(String token) {
		BotEnv botEnv = mock(BotEnv.class);
		when(botEnv.getDiscordToken()).thenReturn(token);
		when(botEnvRepository.findById(BOT_ID)).thenReturn(Optional.of(botEnv));
	}

	private static CompetitionNotice validNotice() {
		return new CompetitionNotice("미니미 대회", "https://tonamel.com/x", "Bo1",
			LocalDateTime.of(2026, 10, 1, 0, 0), LocalDateTime.of(2026, 10, 2, 21, 30),
			LocalDateTime.of(2026, 10, 2, 22, 0), List.of(), List.of());
	}
}
