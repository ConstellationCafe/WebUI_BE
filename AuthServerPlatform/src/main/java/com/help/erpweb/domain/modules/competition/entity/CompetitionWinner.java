package com.help.erpweb.domain.modules.competition.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 대회 우승 칭호({@code Competition.Winners}). 같은 채팅방에서 같은 회원에게 같은 대회·버전 칭호는 하나뿐이다.
 * 우승자가 DiscordUsers에서 지워지면 DB FK(ON DELETE CASCADE)로 함께 지워진다.
 */
@Getter
@Entity
@Table(name = "Winners", catalog = "Competition")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompetitionWinner {
	@EmbeddedId
	private CompetitionWinnerId id;

	/** 대회 개최 날짜(한국 날짜). 대회 매니저가 지정한다. */
	@Column(name = "acquisition", nullable = false)
	private LocalDate acquisition;

	private CompetitionWinner(CompetitionWinnerId id, LocalDate acquisition) {
		this.id = id;
		this.acquisition = acquisition;
	}

	public static CompetitionWinner create(CompetitionWinnerId id, LocalDate acquisition) {
		return new CompetitionWinner(id, acquisition);
	}
}
