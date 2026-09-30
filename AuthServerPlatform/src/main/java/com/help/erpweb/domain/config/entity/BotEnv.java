package com.help.erpweb.domain.config.entity;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * config DB의 봇 실행 환경. WebUI_BE는 대회 공지를 봇 계정으로 게시할 때만 읽으며, 절대 수정하지 않는다.
 * discord_token은 봇 전체 권한을 가진 비밀 값이므로 로그, 응답, toString에 남기지 않는다.
 */
@Getter
@Entity
@Immutable
@Table(name = "bot_env")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BotEnv {
	@Id
	@Column(name = "bot_id", length = 30, nullable = false)
	private String botId;

	@Column(name = "discord_token", length = 255)
	private String discordToken;

	@Column(name = "guild_id")
	private Long guildId;
}
