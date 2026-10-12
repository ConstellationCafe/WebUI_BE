package com.help.erpweb.domain.modules.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import com.help.erpweb.domain.modules.competition.dto.response.CompetitionWinnerPageResponse;
import com.help.erpweb.domain.modules.competition.dto.response.CompetitionWinnerResponse;
import com.help.erpweb.domain.modules.competition.entity.CompetitionWinner;
import com.help.erpweb.domain.modules.competition.entity.CompetitionWinnerId;
import com.help.erpweb.domain.modules.competition.exception.CompetitionMemberNotFoundException;
import com.help.erpweb.domain.modules.competition.exception.CompetitionWinnerConflictException;
import com.help.erpweb.domain.modules.competition.exception.InvalidCompetitionWinnerException;
import com.help.erpweb.domain.modules.competition.repository.CompetitionWinnerRepository;
import com.help.erpweb.domain.modules.competition.type.GameVersion;
import com.help.global.discord.identity.DiscordUser;
import com.help.global.discord.identity.DiscordUserRepository;
import com.help.global.guild.GuildContext;

@ExtendWith(MockitoExtension.class)
class CompetitionWinnerServiceTest {
	private static final String BOT_ID = "bot977062370327298079";
	/** 2026-09-30 00:30 KST (UTC로는 9월 29일) */
	private static final Instant NOW = Instant.parse("2026-09-29T15:30:00Z");
	private static final String S2 = GameVersion.S2.value();

	@Mock
	private CompetitionWinnerRepository winners;
	@Mock
	private DiscordUserRepository users;
	private CompetitionWinnerService service;

	@BeforeEach
	void setUp() {
		service = new CompetitionWinnerService(winners, users, Clock.fixed(NOW, ZoneOffset.UTC));
		GuildContext.setBotId(BOT_ID);
	}

	@AfterEach
	void tearDown() {
		GuildContext.clear();
	}

	@Test
	void grantsTitleToCurrentRoomMemberWithTrimmedValues() {
		givenMember("123");

		CompetitionWinnerResponse response = service.grant(
			" 미니미 Bo1 대회 ", S2, " 123 ", LocalDate.of(2026, 9, 30), "manager-1");

		ArgumentCaptor<CompetitionWinner> saved = ArgumentCaptor.forClass(CompetitionWinner.class);
		verify(winners).saveAndFlush(saved.capture());
		assertThat(saved.getValue().getId())
			.isEqualTo(new CompetitionWinnerId(BOT_ID, "미니미 Bo1 대회", "s2", "123"));
		assertThat(saved.getValue().getAcquisition()).isEqualTo(LocalDate.of(2026, 9, 30));
		assertThat(response.winnerName()).isEqualTo("별");
		assertThat(response.version()).isEqualTo("s2");
	}

	@Test
	void rejectsWinnerWhoIsNotActiveMemberOfCurrentRoom() {
		when(users.findByBotIdAndDiscordID(BOT_ID, "999")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.grant("대회", S2, "999", LocalDate.of(2026, 9, 1), "m"))
			.isInstanceOf(CompetitionMemberNotFoundException.class);
		verify(winners, never()).saveAndFlush(any());
	}

	@Test
	void rejectsSameTitleTwiceInSameRoom() {
		givenMember("123");
		when(winners.existsById(new CompetitionWinnerId(BOT_ID, "대회", "s2", "123"))).thenReturn(true);

		assertThatThrownBy(() -> service.grant("대회", S2, "123", LocalDate.of(2026, 9, 1), "m"))
			.isInstanceOf(CompetitionWinnerConflictException.class);
		verify(winners, never()).saveAndFlush(any());
	}

	@Test
	void rejectsUnknownVersionBlankNameAndFutureKoreanDate() {
		assertThatThrownBy(() -> service.grant("대회", "s9", "123", LocalDate.of(2026, 9, 1), "m"))
			.isInstanceOf(InvalidCompetitionWinnerException.class);
		assertThatThrownBy(() -> service.grant("  ", S2, "123", LocalDate.of(2026, 9, 1), "m"))
			.isInstanceOf(InvalidCompetitionWinnerException.class);
		// 한국 날짜로는 9월 30일이 오늘이라 허용되고, 10월 1일은 미래다.
		assertThatThrownBy(() -> service.grant("대회", S2, "123", LocalDate.of(2026, 10, 1), "m"))
			.isInstanceOf(InvalidCompetitionWinnerException.class);
		verify(users, never()).findByBotIdAndDiscordID(any(), any());
	}

	@Test
	void historyIsScopedToCurrentRoomAndShowsMemberNames() {
		CompetitionWinner first = CompetitionWinner.create(
			new CompetitionWinnerId(BOT_ID, "대회 A", "s2", "123"), LocalDate.of(2026, 9, 30));
		CompetitionWinner second = CompetitionWinner.create(
			new CompetitionWinnerId(BOT_ID, "대회 B", "s2", "456"), LocalDate.of(2026, 9, 1));
		when(winners.findHistory(eq(BOT_ID), eq(PageRequest.of(0, 20))))
			.thenReturn(new PageImpl<>(List.of(first, second), PageRequest.of(0, 20), 2));
		when(users.findAllByBotIdAndDiscordIDIn(BOT_ID, List.of("123", "456")))
			.thenReturn(List.of(DiscordUser.of(BOT_ID, "123", List.of())));

		CompetitionWinnerPageResponse page = service.history(1, 20);

		assertThat(page.items()).extracting(CompetitionWinnerResponse::competitionName)
			.containsExactly("대회 A", "대회 B");
		assertThat(page.items().get(1).winnerName()).isNull();
		assertThat(page.totalElements()).isEqualTo(2);
		assertThat(page.hasNext()).isFalse();
	}

	@Test
	void gameVersionUsesSameValuesAsFrontend() {
		assertThat(GameVersion.S2.value()).isEqualTo("s2");
		assertThat(GameVersion.fromValue("s1")).isEqualTo(GameVersion.S1);
		assertThat(GameVersion.fromValue("S2")).isNull();
	}

	private void givenMember(String discordId) {
		DiscordUser member = DiscordUser.of(BOT_ID, discordId, List.of());
		ReflectionTestUtils.setField(member, "username", "별");
		when(users.findByBotIdAndDiscordID(BOT_ID, discordId)).thenReturn(Optional.of(member));
	}
}
