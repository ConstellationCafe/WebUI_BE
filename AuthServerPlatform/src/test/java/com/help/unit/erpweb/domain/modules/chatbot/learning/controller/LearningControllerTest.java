package com.help.erpweb.domain.modules.chatbot.learning.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.help.erpweb.domain.modules.chatbot.learning.dto.request.repository.LearningDto;
import com.help.erpweb.domain.modules.chatbot.learning.service.LearningService;
import com.help.global.jwt.CustomUser;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LearningControllerTest {
	private LearningService service;
	private LearningController controller;

	@BeforeEach
	void setUp() {
		service = mock(LearningService.class);
		controller = new LearningController(service);
	}

	@Test
	void listSaveAndDeleteEndpointsDelegateAllRequestValues() {
		CustomUser user = mock(CustomUser.class);
		List<LearningDto> payload = List.of();

		controller.getLearningList(user, 2, 40, "ln_key", "word", "ln_key", "ASC");
		controller.saveAll(user, payload);
		controller.deleteAll(user, payload);

		verify(service).getLearningList(user, 2, 40, "ln_key", "word", "ln_key", "ASC");
		verify(service).saveAll(user, payload);
		verify(service).deleteAll(user, payload);
	}
}
