package com.help.erpweb.domain.modules.erp.penalty.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import com.help.erpweb.domain.modules.erp.penalty.dto.request.PenaltyCreateRequest;
import com.help.erpweb.domain.modules.erp.penalty.dto.request.PenaltySort;
import com.help.erpweb.domain.modules.erp.penalty.service.PenaltyService;

import jakarta.validation.Validation;

class PenaltyValidationTest {
	@Test
	void rejectsInvalidIdsAndScoresOtherThanOne() {
		try (var factory = Validation.buildDefaultValidatorFactory()) {
			var validator = factory.getValidator();
			assertThat(validator.validate(request("not-a-uuid", "123", "999", "도배", 1)))
					.isNotEmpty();
			assertThat(validator.validate(request(validId(), "abc", "999", "도배", 1)))
					.isNotEmpty();
			assertThat(validator.validate(request(validId(), "123", "abc", "도배", 1)))
					.isNotEmpty();
			assertThat(validator.validate(request(validId(), "123", "999", "   ", 1)))
					.isNotEmpty();
			assertThat(validator.validate(request(validId(), "123", "999", "도배", 2)))
					.isNotEmpty();
			assertThat(validator.validate(request(validId(), "123", "999", "도배", 1)))
					.isEmpty();
		}
	}

	@Test
	void size101IsRejectedAtControllerBoundary() throws NoSuchMethodException {
		try (var factory = Validation.buildDefaultValidatorFactory()) {
			var validator = factory.getValidator().forExecutables();
			var controller = new PenaltyController(mock(PenaltyService.class));
			Method method = PenaltyController.class.getMethod("history", String.class,
					String.class, PenaltySort.class, int.class, int.class);
			assertThat(validator.validateParameters(controller, method,
					new Object[]{null, null, PenaltySort.OCCURRED_AT_DESC, 1, 101})).isNotEmpty();
		}
	}

	private PenaltyCreateRequest request(String id, String target, String channel,
											String reason, int score) {
		return new PenaltyCreateRequest(id, target, channel, null, reason, score, null);
	}

	private String validId() {
		return "3f2b8c1e-7b0c-4036-8a9d-29947cbe1691";
	}
}
