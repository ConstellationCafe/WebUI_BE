package com.help.erpweb.domain.modules.erp.penalty.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PenaltyLogTest {
	@Test
	void repeatCancellationPreservesOriginalAudit() {
		PenaltyLog log = new PenaltyLog();
		ReflectionTestUtils.setField(log, "status", PenaltyStatus.ACTIVE);
		Instant firstTime = Instant.parse("2026-09-28T00:44:00Z");

		log.cancel("admin-1", firstTime, "정정");
		log.cancel("admin-2", firstTime.plusSeconds(10), "다른 취소 이유");

		assertThat(log.getStatus()).isEqualTo(PenaltyStatus.CANCELED);
		assertThat(log.getCanceledByDiscordId()).isEqualTo("admin-1");
		assertThat(log.canceledInstant()).isEqualTo(firstTime);
		assertThat(log.getCancellationReason()).isEqualTo("정정");
	}
}
