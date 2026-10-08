package com.help.erpweb.domain.modules.chatbot.menu.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.help.erpweb.domain.modules.chatbot.menu.dto.request.repository.MenuDto;
import com.help.erpweb.domain.modules.chatbot.menu.service.MenuService;
import com.help.global.jwt.CustomUser;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MenuControllerTest {
	private MenuService service;
	private MenuController controller;

	@BeforeEach
	void setUp() {
		service = mock(MenuService.class);
		controller = new MenuController(service);
	}

	@Test
	void listSaveAndDeleteEndpointsDelegateAllRequestValues() {
		CustomUser user = mock(CustomUser.class);
		List<MenuDto> payload = List.of();

		controller.getMenuList(user, 2, 40, "mn_value", "word", "mn_value", "ASC");
		controller.saveAll(user, payload);
		controller.deleteAll(user, payload);

		verify(service).getMenuList(user, 2, 40, "mn_value", "word", "mn_value", "ASC");
		verify(service).saveAll(user, payload);
		verify(service).deleteAll(user, payload);
	}
}
