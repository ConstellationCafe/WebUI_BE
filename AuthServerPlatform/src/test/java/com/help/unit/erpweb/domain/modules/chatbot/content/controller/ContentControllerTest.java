package com.help.erpweb.domain.modules.chatbot.content.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.help.erpweb.domain.modules.chatbot.content.dto.request.repository.ContentDto;
import com.help.erpweb.domain.modules.chatbot.content.service.ContentService;
import com.help.global.jwt.CustomUser;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ContentControllerTest {
	private ContentService service;
	private ContentController controller;

	@BeforeEach
	void setUp() {
		service = mock(ContentService.class);
		controller = new ContentController(service);
	}

	@Test
	void listSaveAndDeleteEndpointsDelegateAllRequestValues() {
		CustomUser user = mock(CustomUser.class);
		List<ContentDto> payload = List.of();

		controller.getContentList(user, 2, 40, "cn_value", "word", "cn_value", "ASC");
		controller.saveAll(user, payload);
		controller.deleteAll(user, payload);

		verify(service).getContentList(user, 2, 40, "cn_value", "word", "cn_value", "ASC");
		verify(service).saveAll(user, payload);
		verify(service).deleteAll(user, payload);
	}
}
