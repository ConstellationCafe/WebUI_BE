package com.help.erpweb.domain.modules.chatbot.music.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.help.erpweb.domain.modules.chatbot.music.dto.request.repository.MusicDto;
import com.help.erpweb.domain.modules.chatbot.music.service.MusicService;
import com.help.global.jwt.CustomUser;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MusicControllerTest {
	private MusicService service;
	private MusicController controller;

	@BeforeEach
	void setUp() {
		service = mock(MusicService.class);
		controller = new MusicController(service);
	}

	@Test
	void listSaveAndDeleteEndpointsDelegateAllRequestValues() {
		CustomUser user = mock(CustomUser.class);
		List<MusicDto> payload = List.of();

		controller.getMusicList(user, 2, 40, "video_id", "word", "video_id", "ASC");
		controller.saveAll(user, payload);
		controller.deleteAll(user, payload);

		verify(service).getMusicList(user, 2, 40, "video_id", "word", "video_id", "ASC");
		verify(service).saveAll(user, payload);
		verify(service).deleteAll(user, payload);
	}
}
