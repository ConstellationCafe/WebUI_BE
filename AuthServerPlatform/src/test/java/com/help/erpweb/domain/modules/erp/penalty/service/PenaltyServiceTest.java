package com.help.erpweb.domain.modules.erp.penalty.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.help.erpweb.domain.modules.erp.penalty.dto.request.PenaltySort;
import com.help.erpweb.domain.modules.erp.penalty.entity.PenaltyLog;
import com.help.erpweb.domain.modules.erp.penalty.repository.PenaltyMember;
import com.help.erpweb.domain.modules.erp.penalty.repository.PenaltyRepository;
import com.help.global.common.exception.CustomException;
import com.help.global.guild.GuildContext;

@ExtendWith(MockitoExtension.class)
class PenaltyServiceTest {
	private static final Instant NOW = Instant.parse("2026-09-28T00:44:00Z");
	private static final String REQUEST_ID = "3f2b8c1e-7b0c-4036-8a9d-29947cbe1691";
	private static final PenaltyMember MEMBER = new PenaltyMember("123", "별", "재적", "sk");

	@Mock
	private PenaltyRepository repository;
	private PenaltyService service;

	@BeforeEach
	void setUp() {
		service = new PenaltyService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
		GuildContext.setBotId("bot-a");
	}

	@AfterEach
	void tearDown() {
		GuildContext.clear();
	}

	@Test
	void recentWindowIncludesExactThirtyDayBoundaryAndExcludesNow() {
		when(repository.countRankedMembers(eq("bot-a"), eq(null), any(), any())).thenReturn(0L);

		service.ranking(null, 1, 20);

		verify(repository).countRankedMembers("bot-a", null,
				Instant.parse("2026-08-29T00:44:00Z"), NOW);
		verify(repository).findRankedMembers("bot-a", null,
				Instant.parse("2026-08-29T00:44:00Z"), NOW, 1, 20);
	}

	@Test
	void historyAggregatesDistinctTargetsOncePerPage() {
		PenaltyLog first = mock(PenaltyLog.class);
		PenaltyLog second = mock(PenaltyLog.class);
		PenaltyLog third = mock(PenaltyLog.class);
		when(first.getTargetDiscordId()).thenReturn("123");
		when(second.getTargetDiscordId()).thenReturn("123");
		when(third.getTargetDiscordId()).thenReturn("456");
		when(repository.findHistory("bot-a", "999", null,
				PenaltySort.OCCURRED_AT_DESC, 1, 20)).thenReturn(List.of(first, second, third));
		when(repository.findCumulativeScores("bot-a", List.of("123", "456"),
				NOW.minusSeconds(30L * 24 * 3600), NOW)).thenReturn(Map.of("123", 2L, "456", 1L));

		var response = service.history("999", null, PenaltySort.OCCURRED_AT_DESC, 1, 20);

		assertThat(response.items()).extracting(item -> item.targetCumulativeScore30d())
				.containsExactly(2L, 2L, 1L);
		verify(repository, times(1)).findCumulativeScores(eq("bot-a"), eq(List.of("123", "456")),
				any(), any());
	}

	@Test
	void guildContextIsUsedForMemberAndHistoryLookups() {
		when(repository.findActiveMember("bot-a", "123", false)).thenReturn(MEMBER);
		service.member("123", 1, 20);
		verify(repository).countHistory("bot-a", null, "123");

		GuildContext.setBotId("bot-b");
		assertThatThrownBy(() -> service.member("123", 1, 20))
				.isInstanceOf(CustomException.class).hasMessageContaining("재적 회원");
		verify(repository).findActiveMember("bot-b", "123", false);
		verify(repository, never()).countHistory("bot-b", null, "123");
	}

	@Test
	void nonMemberIsRejectedBeforeInsert() {
		assertThatThrownBy(() -> service.create(command(null)))
				.isInstanceOf(CustomException.class).hasMessageContaining("재적 회원");
		verify(repository, never()).insertIfAbsent(any(), any(), any(), any(), any(),
				any(), org.mockito.ArgumentMatchers.anyInt(), any(), any(), any(), any());
	}

	@Test
	void futureOccurrenceIsRejectedBeforeMemberLookup() {
		assertThatThrownBy(() -> service.create(command(NOW.plusSeconds(1))))
				.isInstanceOf(CustomException.class).hasMessageContaining("발생 시각");
		assertThatThrownBy(() -> service.create(command(NOW.plusNanos(1))))
				.isInstanceOf(CustomException.class).hasMessageContaining("발생 시각");
		verify(repository, never()).findActiveMember(any(), any(), org.mockito.ArgumentMatchers.anyBoolean());
	}

	@Test
	void sameRequestIdAndContentReusesStoredPenalty() {
		prepareStoredPenalty("도배");
		when(repository.findActiveMember("bot-a", "123", false)).thenReturn(MEMBER);
		var result = service.create(command(null));

		assertThat(result.discordId()).isEqualTo("123");
		verify(repository, never()).insertIfAbsent(any(), any(), any(), any(), any(),
				any(), org.mockito.ArgumentMatchers.anyInt(), any(), any(), any(), any());
		verify(repository).findByRequestId("bot-a", REQUEST_ID);
	}

	@Test
	void firstRequestInsertsAndReturnsItsCurrentDetail() {
		PenaltyLog stored = storedPenalty("도배");
		when(repository.findByRequestId("bot-a", REQUEST_ID)).thenReturn(null, stored);
		when(repository.findActiveMember("bot-a", "123", true)).thenReturn(MEMBER);

		var result = service.create(command(null));

		assertThat(result.discordId()).isEqualTo("123");
		verify(repository).insertIfAbsent("bot-a", MEMBER, REQUEST_ID,
				"999", null, "도배", 1, "admin", NOW, null, NOW);
	}

	@Test
	void sameRequestIdWithDifferentContentConflicts() {
		prepareStoredPenalty("다른 사유");
		assertThatThrownBy(() -> service.create(command(null)))
				.isInstanceOf(CustomException.class).hasMessageContaining("다른 벌점 내용");
		verify(repository, never()).countHistory(any(), any(), any());
	}

	@Test
	void cancellationRetainsAuditAndRecomputesScore() {
		PenaltyLog log = mock(PenaltyLog.class);
		when(log.getTargetDiscordId()).thenReturn("123");
		when(repository.lockById("bot-a", 42L)).thenReturn(log);
		when(repository.findActiveMember("bot-a", "123", false)).thenReturn(MEMBER);
		when(repository.findCumulativeScores(eq("bot-a"), any(),
				any(), eq(NOW))).thenReturn(Map.of());

		var result = service.cancel(42L, "admin", " 잘못 부여 ");

		verify(log).cancel("admin", NOW, "잘못 부여");
		verify(repository).flush();
		assertThat(result.cumulativeScore30d()).isZero();
	}

	@Test
	void otherGuildPenaltyCannotBeCanceled() {
		assertThatThrownBy(() -> service.cancel(42L, "admin", "정정"))
				.isInstanceOf(CustomException.class).hasMessageContaining("벌점 내역");
		verify(repository).lockById("bot-a", 42L);
		verify(repository, never()).flush();
	}

	private PenaltyCreateCommand command(Instant occurredAt) {
		return new PenaltyCreateCommand(REQUEST_ID, "123", "999", null,
				"도배", 1, occurredAt, "admin");
	}

	private void prepareStoredPenalty(String reason) {
		PenaltyLog stored = storedPenalty(reason);
		when(repository.findByRequestId("bot-a", REQUEST_ID)).thenReturn(stored);
	}

	private PenaltyLog storedPenalty(String reason) {
		PenaltyLog stored = mock(PenaltyLog.class);
		when(stored.getTargetDiscordId()).thenReturn("123");
		when(stored.getChannelId()).thenReturn("999");
		when(stored.getReason()).thenReturn(reason);
		lenient().when(stored.getScore()).thenReturn(1);
		return stored;
	}
}
