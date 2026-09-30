package com.help.erpweb.domain.modules.competition.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** {@code Competition.Winners} PK. 0006 migration 이후 (bot_id, competition_name, version, winner). */
@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CompetitionWinnerId implements Serializable {
	@Column(name = "bot_id", length = 30, nullable = false)
	private String botId;

	@Column(name = "competition_name", length = 100, nullable = false)
	private String competitionName;

	/** {@code GameVersion.value()} */
	@Column(name = "version", length = 2, nullable = false)
	private String version;

	/** 우승자 Discord ID */
	@Column(name = "winner", length = 32, nullable = false)
	private String winner;
}
