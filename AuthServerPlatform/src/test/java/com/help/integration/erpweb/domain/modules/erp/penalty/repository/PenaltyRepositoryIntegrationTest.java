package com.help.erpweb.domain.modules.erp.penalty.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.util.ReflectionTestUtils;

import com.help.erpweb.domain.modules.erp.penalty.dto.request.PenaltySort;

import jakarta.persistence.EntityManager;

class PenaltyRepositoryIntegrationTest {
	private static final Instant NOW = Instant.parse("2026-09-28T00:44:00Z");
	private static final Instant START = NOW.minusSeconds(30L * 24 * 60 * 60);

	private LocalContainerEntityManagerFactoryBean factory;
	private EntityManager entityManager;
	private JdbcTemplate jdbc;
	private PenaltyRepository repository;

	@BeforeEach
	void setUp() {
		String databaseName = "penalty_" + UUID.randomUUID().toString().replace("-", "");
		var source = new DriverManagerDataSource(
				"jdbc:h2:mem:" + databaseName + ";MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
				"sa", "");
		jdbc = new JdbcTemplate(source);
		jdbc.execute("CREATE SCHEMA Constellation_Network");
		jdbc.execute("""
				CREATE TABLE Constellation_Network.DiscordUsers (
					bot_id VARCHAR(30), discordID VARCHAR(20), username VARCHAR(100), state VARCHAR(16))
				""");
		jdbc.execute("""
				CREATE TABLE Constellation_Network.PenaltyLog (
					id BIGINT AUTO_INCREMENT PRIMARY KEY, bot_id VARCHAR(30) NOT NULL,
					target_discord_id VARCHAR(20) NOT NULL,
					target_username VARCHAR(100) NOT NULL, channel_id VARCHAR(20) NOT NULL,
					channel_name VARCHAR(100), reason VARCHAR(255) NOT NULL,
					score INT NOT NULL, issuer_discord_id VARCHAR(20) NOT NULL,
					occurred_at DATETIME(3) NOT NULL, requested_occurred_at DATETIME(3),
					created_at DATETIME(3) NOT NULL, status VARCHAR(16) NOT NULL,
					request_id CHAR(36) NOT NULL, canceled_by_discord_id VARCHAR(20),
					canceled_at DATETIME(3), cancellation_reason VARCHAR(255))
				""");

		factory = new LocalContainerEntityManagerFactoryBean();
		factory.setDataSource(source);
		factory.setPackagesToScan("com.help.erpweb.domain.modules.erp.penalty.entity");
		factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
		factory.setPersistenceUnitName("constellation");
		factory.afterPropertiesSet();
		entityManager = factory.getObject().createEntityManager();
		repository = new PenaltyRepository();
		ReflectionTestUtils.setField(repository, "entityManager", entityManager);
	}

	@AfterEach
	void tearDown() {
		entityManager.close();
		factory.destroy();
	}

	@Test
	void thirtyDayWindowIncludesStartExcludesOlderAndNowAndOtherGuild() {
		member("bot-a", "123", "재적");
		member("bot-b", "123", "재적");
		log(1, "bot-a", "123", START.minusMillis(1), "ACTIVE");
		log(2, "bot-a", "123", START, "ACTIVE");
		log(3, "bot-a", "123", NOW.minusMillis(1), "ACTIVE");
		log(4, "bot-a", "123", NOW, "ACTIVE");
		log(5, "bot-a", "123", START, "CANCELED");
		log(6, "bot-b", "123", START, "ACTIVE");

		assertThat(repository.findCumulativeScores("bot-a", List.of("123"), START, NOW))
				.containsEntry("123", 2L).hasSize(1);
		assertThat(repository.findCumulativeScores("bot-b", List.of("123"), START, NOW))
				.containsEntry("123", 1L).hasSize(1);
		assertThat(repository.findHistory("bot-a", null, "123",
				PenaltySort.OCCURRED_AT_DESC, 1, 20)).hasSize(5);
	}

	@Test
	void rankingContainsOnlyActiveMembersAndOrdersByScoreThenDiscordId() {
		member("bot-a", "123", "재적");
		member("bot-a", "456", "재적");
		member("bot-a", "789", "탈퇴");
		log(1, "bot-a", "123", START, "ACTIVE");
		log(2, "bot-a", "123", NOW.minusMillis(1), "ACTIVE");
		log(3, "bot-a", "456", START, "ACTIVE");
		log(4, "bot-a", "456", NOW.minusMillis(1), "ACTIVE");
		log(5, "bot-a", "789", START, "ACTIVE");

		assertThat(repository.countRankedMembers("bot-a", null, START, NOW)).isEqualTo(2);
		var items = repository.findRankedMembers("bot-a", null, START, NOW, 1, 20);
		assertThat(items).extracting(item -> item.discordId()).containsExactly("123", "456");
		assertThat(items).extracting(item -> item.cumulativeScore30d())
				.containsExactly(2L, 2L);
		assertThat(repository.countRankedMembers("bot-a", "45", START, NOW)).isEqualTo(1);
	}

	@Test
	void activeDiscordMemberWithoutUsersRowCanReceivePenalty() {
		member("bot-a", "123", "재적");
		member("bot-b", "456", "재적");
		member("bot-a", "789", "탈퇴");

		assertThat(repository.findActiveMember("bot-a", "123", false))
				.extracting(PenaltyMember::discordId).isEqualTo("123");
		assertThat(repository.findActiveMember("bot-a", "456", false)).isNull();
		assertThat(repository.findActiveMember("bot-a", "789", false)).isNull();

		entityManager.getTransaction().begin();
		PenaltyMember target = repository.findActiveMember("bot-a", "123", true);
		repository.insertIfAbsent("bot-a", target, UUID.randomUUID().toString(),
				"999", "자유채팅", "도배", 1, "900", NOW.minusSeconds(1), null, NOW);
		entityManager.getTransaction().commit();

		assertThat(repository.findHistory("bot-a", null, "123",
				PenaltySort.OCCURRED_AT_DESC, 1, 20)).hasSize(1);
		assertThat(repository.findCumulativeScores("bot-a", List.of("123"), START, NOW))
				.containsEntry("123", 1L);
		assertThat(repository.countRankedMembers("bot-a", null, START, NOW)).isEqualTo(1);
	}


	@Test
	void historyUsesIdAsStableTieBreakerWhenOccurrenceTimesMatch() {
		member("bot-a", "123", "재적");
		Instant sameTime = NOW.minusSeconds(10);
		log(11, "bot-a", "123", sameTime, "ACTIVE");
		log(12, "bot-a", "123", sameTime, "ACTIVE");

		var descending = repository.findHistory("bot-a", null, "123",
				PenaltySort.OCCURRED_AT_DESC, 1, 20);
		var ascending = repository.findHistory("bot-a", null, "123",
				PenaltySort.OCCURRED_AT_ASC, 1, 20);

		assertThat(descending).extracting(item -> item.getId()).containsExactly(12L, 11L);
		assertThat(ascending).extracting(item -> item.getId()).containsExactly(11L, 12L);
	}

	private void member(String botId, String discordId, String state) {
		jdbc.update("""
				INSERT INTO Constellation_Network.DiscordUsers
				(bot_id, discordID, username, state) VALUES (?, ?, ?, ?)
				""", botId, discordId, "별", state);
	}

	private void log(long id, String botId, String target,
					 Instant occurredAt, String status) {
		Timestamp timestamp = Timestamp.from(occurredAt);
		jdbc.update("""
				INSERT INTO Constellation_Network.PenaltyLog
				(id, bot_id, target_discord_id, target_username, channel_id,
				 reason, score, issuer_discord_id, occurred_at, created_at, status, request_id)
				VALUES (?, ?, ?, '별', '999', '도배', 1, '900', ?, ?, ?, ?)
				""", id, botId, target, timestamp, timestamp, status,
				UUID.randomUUID().toString());
	}
}
