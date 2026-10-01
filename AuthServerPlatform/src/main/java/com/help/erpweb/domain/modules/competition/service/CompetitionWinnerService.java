package com.help.erpweb.domain.modules.competition.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 대회 우승 칭호 부여와 이력. 모든 기록은 현재 채팅방(botId)으로 제한한다.
 * 우승자는 현재 채팅방의 재적 회원이어야 하며, 같은 대회·버전 칭호는 회원마다 하나뿐이다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompetitionWinnerService {
	static final int COMPETITION_NAME_MAX = 100;

	private final CompetitionWinnerRepository winnerRepository;
	private final DiscordUserRepository discordUserRepository;
	private final Clock clock;

	@Transactional(transactionManager = "constellationTransactionManager")
	public CompetitionWinnerResponse grant(
		String competitionName,
		String version,
		String winnerDiscordId,
		LocalDate acquisition,
		String managerId
	) {
		final String botId = GuildContext.requireBotId();
		final String name = requireCompetitionName(competitionName);
		final GameVersion gameVersion = requireVersion(version);
		final String winner = winnerDiscordId == null ? "" : winnerDiscordId.strip();
		requireAcquisition(acquisition);

		final DiscordUser member = discordUserRepository.findByBotIdAndDiscordID(botId, winner)
			.orElseThrow(CompetitionMemberNotFoundException::new);

		final CompetitionWinnerId id = new CompetitionWinnerId(botId, name, gameVersion.value(), winner);
		if (winnerRepository.existsById(id)) {
			throw new CompetitionWinnerConflictException();
		}
		try {
			winnerRepository.saveAndFlush(CompetitionWinner.create(id, acquisition));
		} catch (DataIntegrityViolationException ex) {
			// 동시에 같은 칭호를 부여한 경우. 0006 migration의 PK 교체 전에는 다른 채팅방의 같은 칭호도 여기에 걸린다.
			throw new CompetitionWinnerConflictException();
		}
		log.info("대회 우승 칭호 부여 - botId={}, competition={}, version={}, winner={}, managerId={}",
			botId, name, gameVersion.value(), winner, managerId);
		return new CompetitionWinnerResponse(name, gameVersion.value(), winner, member.getNickname(), acquisition);
	}

	@Transactional(transactionManager = "constellationTransactionManager", readOnly = true)
	public CompetitionWinnerPageResponse history(int page, int size) {
		final String botId = GuildContext.requireBotId();
		final Page<CompetitionWinner> result = winnerRepository.findHistory(botId, PageRequest.of(page - 1, size));
		final List<String> winnerIds = result.getContent().stream()
			.map(winner -> winner.getId().getWinner())
			.distinct()
			.toList();
		final Map<String, DiscordUser> members = winnerIds.isEmpty()
			? Map.of()
			: discordUserRepository.findAllByBotIdAndDiscordIDIn(botId, winnerIds).stream()
				.collect(Collectors.toMap(DiscordUser::getDiscordID, Function.identity(), (first, second) -> first));

		final List<CompetitionWinnerResponse> items = result.getContent().stream()
			.map(winner -> {
				final DiscordUser member = members.get(winner.getId().getWinner());
				return new CompetitionWinnerResponse(
					winner.getId().getCompetitionName(),
					winner.getId().getVersion(),
					winner.getId().getWinner(),
					member == null ? null : member.getNickname(),
					winner.getAcquisition());
			})
			.toList();
		return new CompetitionWinnerPageResponse(
			items, page, size, result.getTotalElements(), result.getTotalPages(), result.hasNext());
	}

	private static String requireCompetitionName(String competitionName) {
		final String name = competitionName == null ? "" : competitionName.strip();
		if (name.isEmpty()) {
			throw new InvalidCompetitionWinnerException("대회명을 입력해 주세요.");
		}
		if (name.length() > COMPETITION_NAME_MAX) {
			throw new InvalidCompetitionWinnerException("대회명은 " + COMPETITION_NAME_MAX + "자 이하로 입력해 주세요.");
		}
		return name;
	}

	private static GameVersion requireVersion(String version) {
		final GameVersion gameVersion = GameVersion.fromValue(version);
		if (gameVersion == null) {
			throw new InvalidCompetitionWinnerException("지원하지 않는 게임 버전입니다.");
		}
		return gameVersion;
	}

	/** 대회 개최 날짜는 한국 날짜 기준 오늘까지만 받는다. */
	private void requireAcquisition(LocalDate acquisition) {
		if (acquisition == null) {
			throw new InvalidCompetitionWinnerException("대회 개최 날짜를 입력해 주세요.");
		}
		if (acquisition.isAfter(LocalDate.now(clock.withZone(CompetitionNotice.ZONE)))) {
			throw new InvalidCompetitionWinnerException("대회 개최 날짜는 오늘 이전이어야 합니다.");
		}
	}
}
