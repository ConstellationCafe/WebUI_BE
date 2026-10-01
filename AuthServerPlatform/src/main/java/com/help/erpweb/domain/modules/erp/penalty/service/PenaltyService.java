package com.help.erpweb.domain.modules.erp.penalty.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.help.erpweb.domain.modules.erp.penalty.dto.request.PenaltySort;
import com.help.erpweb.domain.modules.erp.penalty.dto.response.PenaltyHistoryItemResponse;
import com.help.erpweb.domain.modules.erp.penalty.dto.response.PenaltyMemberDetailResponse;
import com.help.erpweb.domain.modules.erp.penalty.dto.response.PenaltyMemberRankResponse;
import com.help.erpweb.domain.modules.erp.penalty.dto.response.PenaltyPageResponse;
import com.help.erpweb.domain.modules.erp.penalty.entity.PenaltyLog;
import com.help.erpweb.domain.modules.erp.penalty.repository.PenaltyMember;
import com.help.erpweb.domain.modules.erp.penalty.repository.PenaltyRepository;
import com.help.global.common.exception.CustomException;
import com.help.global.common.exception.ErrorCode;
import com.help.global.guild.GuildContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PenaltyService {
	private static final int DETAIL_PAGE_SIZE = 20;
	private static final int WINDOW_DAYS = 30;

	private final PenaltyRepository repository;
	private final Clock clock;

	@Transactional(transactionManager = "constellationTransactionManager")
	public PenaltyMemberDetailResponse create(PenaltyCreateCommand command) {
		String botId = GuildContext.requireBotId();
		Instant rawNow = clock.instant();
		Instant now = rawNow.truncatedTo(ChronoUnit.MILLIS);
		Instant requestedAt = command.occurredAt() == null
				? null : command.occurredAt().truncatedTo(ChronoUnit.MILLIS);
		if (command.score() != 1) {
			throw new CustomException(ErrorCode.PENALTY_INVALID_SCORE);
		}
		if (command.occurredAt() != null && command.occurredAt().isAfter(rawNow)) {
			throw new CustomException(ErrorCode.PENALTY_INVALID_OCCURRED_AT);
		}
		String requestId = UUID.fromString(command.requestId()).toString();
		String channelName = command.channelName() == null || command.channelName().isBlank()
				? null : command.channelName().trim();
		String reason = command.reason().trim();
		PenaltyLog existing = repository.findByRequestId(botId, requestId);
		if (existing != null) {
			checkSameRequest(existing, command, channelName, reason, requestedAt);
			PenaltyMember member = requireMember(botId, command.targetDiscordId(), false);
			return detail(botId, member, 1, DETAIL_PAGE_SIZE, now);
		}
		PenaltyMember member = requireMember(botId, command.targetDiscordId(), true);
		repository.insertIfAbsent(botId, member, requestId, command.channelId(), channelName,
				reason, command.score(), command.issuerDiscordId(),
				requestedAt == null ? now : requestedAt, requestedAt, now);
		PenaltyLog stored = repository.findByRequestId(botId, requestId);
		checkSameRequest(stored, command, channelName, reason, requestedAt);
		// A newly created penalty at 'now' must be visible in the response's exclusive upper bound.
		return detail(botId, member, 1, DETAIL_PAGE_SIZE, now.plusMillis(1));
	}

	private void checkSameRequest(PenaltyLog stored, PenaltyCreateCommand command,
									String channelName, String reason, Instant requestedAt) {
		if (!Objects.equals(stored.getTargetDiscordId(), command.targetDiscordId())
				|| !Objects.equals(stored.getChannelId(), command.channelId())
				|| !Objects.equals(stored.getChannelName(), channelName)
				|| !Objects.equals(stored.getReason(), reason)
				|| stored.getScore() != command.score()
				|| !Objects.equals(stored.requestedOccurredInstant(), requestedAt)) {
			throw new CustomException(ErrorCode.PENALTY_REQUEST_CONFLICT);
		}
	}

	@Transactional(transactionManager = "constellationTransactionManager")
	public PenaltyMemberDetailResponse cancel(long penaltyId, String issuer, String reason) {
		String botId = GuildContext.requireBotId();
		PenaltyLog log = repository.lockById(botId, penaltyId);
		if (log == null) {
			throw new CustomException(ErrorCode.PENALTY_NOT_FOUND);
		}
		PenaltyMember member = requireMember(botId, log.getTargetDiscordId(), false);
		Instant now = clock.instant().truncatedTo(ChronoUnit.MILLIS);
		log.cancel(issuer, now, reason.trim());
		repository.flush();
		return detail(botId, member, 1, DETAIL_PAGE_SIZE, now);
	}

	@Transactional(transactionManager = "constellationTransactionManager", readOnly = true)
	public PenaltyPageResponse<PenaltyHistoryItemResponse> history(
			String channelId, String discordId, PenaltySort sort, int page, int size) {
		String botId = GuildContext.requireBotId();
		Instant now = clock.instant();
		return historyPage(botId, channelId, discordId, sort, page, size, now);
	}

	@Transactional(transactionManager = "constellationTransactionManager", readOnly = true)
	public PenaltyPageResponse<PenaltyMemberRankResponse> ranking(String discordId, int page, int size) {
		String botId = GuildContext.requireBotId();
		Instant now = clock.instant();
		Instant since = now.minus(WINDOW_DAYS, ChronoUnit.DAYS);
		long count = repository.countRankedMembers(botId, discordId, since, now);
		List<PenaltyMemberRankResponse> items = repository.findRankedMembers(
				botId, discordId, since, now, page, size);
		return PenaltyPageResponse.of(items, page, size, count);
	}

	@Transactional(transactionManager = "constellationTransactionManager", readOnly = true)
	public PenaltyMemberDetailResponse member(String discordId, int page, int size) {
		String botId = GuildContext.requireBotId();
		PenaltyMember member = requireMember(botId, discordId, false);
		return detail(botId, member, page, size, clock.instant());
	}

	private PenaltyMemberDetailResponse detail(String botId, PenaltyMember member,
												int page, int size, Instant now) {
		PenaltyPageResponse<PenaltyHistoryItemResponse> history = historyPage(
				botId, null, member.discordId(), PenaltySort.OCCURRED_AT_DESC, page, size, now);
		long score = repository.findCumulativeScores(botId, List.of(member.discordId()),
				now.minus(WINDOW_DAYS, ChronoUnit.DAYS), now)
				.getOrDefault(member.discordId(), 0L);
		return new PenaltyMemberDetailResponse(member.discordId(), member.username(),
				member.state(), score, history);
	}

	private PenaltyPageResponse<PenaltyHistoryItemResponse> historyPage(
			String botId, String channelId, String discordId,
			PenaltySort sort, int page, int size, Instant now) {
		long count = repository.countHistory(botId, channelId, discordId);
		List<PenaltyLog> logs = repository.findHistory(botId, channelId, discordId, sort, page, size);
		List<String> targetIds = logs.stream().map(PenaltyLog::getTargetDiscordId).distinct().toList();
		Map<String, Long> scores = repository.findCumulativeScores(botId, targetIds,
				now.minus(WINDOW_DAYS, ChronoUnit.DAYS), now);
		List<PenaltyHistoryItemResponse> items = logs.stream().map(log ->
				PenaltyHistoryItemResponse.of(log, scores.getOrDefault(log.getTargetDiscordId(), 0L)))
				.toList();
		return PenaltyPageResponse.of(items, page, size, count);
	}

	private PenaltyMember requireMember(String botId, String discordId, boolean lock) {
		PenaltyMember member = repository.findActiveMember(botId, discordId, lock);
		if (member == null) {
			throw new CustomException(ErrorCode.PENALTY_MEMBER_NOT_FOUND);
		}
		return member;
	}
}
