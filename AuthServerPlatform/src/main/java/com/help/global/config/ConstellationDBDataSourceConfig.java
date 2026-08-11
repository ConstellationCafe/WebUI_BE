package com.help.global.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/** JPA와 DB를 연결하는 코드 */
@Configuration
@Profile("!test")
@EnableTransactionManagement
@EnableJpaRepositories(
	basePackages = {
		"com.help.authserver.domain.user.repository.constellation",
		"com.help.backend.domain"
	},
	entityManagerFactoryRef = "constellationEntityManagerFactory",
	transactionManagerRef = "constellationTransactionManager"
)
public class ConstellationDBDataSourceConfig {
	@Value("${spring.datasource.constellation.url}")
	private String dbUrl;

	@Value("${spring.datasource.constellation.username}")
	private String dbUser;

	@Value("${spring.datasource.constellation.password}")
	private String dbPassword;

	@Primary
	@Bean(name = "constellationDataSource")  // DB 연결 정보
	public DataSource dataSource() {
		final HikariConfig config = new HikariConfig();
		config.setJdbcUrl(dbUrl);
		config.setUsername(dbUser);
		config.setPassword(dbPassword);
		config.setDriverClassName("com.mysql.cj.jdbc.Driver");
		config.setTransactionIsolation("TRANSACTION_READ_COMMITTED");

		config.setMaximumPoolSize(10);                 // 기본: 10
		config.setMinimumIdle(3);                      // 기본: same as max
		config.setIdleTimeout(300_000);                // 5분
		config.setMaxLifetime(600_000);                // 10분 (MySQL 서버 timeout보다 작게)
		config.setConnectionTimeout(30_000);           // 커넥션 대기 최대 30초
		config.setValidationTimeout(5_000);            // 커넥션 검증 제한 시간
		config.setKeepaliveTime(300_000);              // 5분마다 ping

		return new HikariDataSource(config);
	}

	@Primary
	@Bean(name = "constellationEntityManagerFactory")  // JPA 엔티티 매니저
	public LocalContainerEntityManagerFactoryBean entityManagerFactory(
		EntityManagerFactoryBuilder builder,
		@Qualifier("constellationDataSource") DataSource dataSource) {
		final Map<String, Object> properties = new HashMap<>();
		properties.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
		properties.put("hibernate.show_sql", true);
		properties.put("hibernate.hbm2ddl.auto", "none");
		 properties.put("hibernate.physical_naming_strategy",
		 	"org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl");

		return builder
			.dataSource(dataSource)
			.packages(
				"com.help.authserver.domain.user.entity",
				"com.help.backend.domain"
			)
			.persistenceUnit("constellation")
			.properties(properties)
			.build();
	}

	@Primary
	@Bean(name = "constellationTransactionManager")  // 트랜잭션 관리
	public PlatformTransactionManager transactionManager(
		@Qualifier("constellationEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
		return new JpaTransactionManager(entityManagerFactory);
	}
}
