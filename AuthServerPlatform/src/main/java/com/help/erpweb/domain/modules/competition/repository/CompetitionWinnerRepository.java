package com.help.erpweb.domain.modules.competition.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.help.erpweb.domain.modules.competition.entity.CompetitionWinner;
import com.help.erpweb.domain.modules.competition.entity.CompetitionWinnerId;

public interface CompetitionWinnerRepository extends JpaRepository<CompetitionWinner, CompetitionWinnerId> {

	/** 현재 채팅방의 칭호 부여 이력. 최근 대회 날짜 순, 같은 날은 대회명·우승자 순. */
	@Query(
		value = """
			SELECT w
			FROM CompetitionWinner w
			WHERE w.id.botId = :botId
			ORDER BY w.acquisition DESC, w.id.competitionName ASC, w.id.winner ASC
			""",
		countQuery = "SELECT COUNT(w) FROM CompetitionWinner w WHERE w.id.botId = :botId"
	)
	Page<CompetitionWinner> findHistory(@Param("botId") String botId, Pageable pageable);
}
