package com.help.backend.domain.repository.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import com.help.backend.domain.repository.dto.request.repository.LearningDto;
import com.help.backend.domain.repository.service.LearningService;
import com.help.backend.global.common.response.ApiResponse;
import com.help.backend.global.jwt.CustomUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

// 컨트롤러 클래스명에 맞게 수정하세요.
@WebMvcTest(LearningController.class)
// 보안 설정이 따로 있으면 여기서 Import 하거나, 반대로 필터를 끄는 옵션을 써야 할 수 있음.
// @AutoConfigureMockMvc(addFilters = false)  // <- 필요하면 이걸로 시큐리티 필터 끄기
class LearningControllerWebMvcTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean
    LearningService learningService;

//    @Test
//    void getLearningList_returnsSuccessResponse() throws Exception {
//        // given
//        List<LearningDto> dtoList = List.of(
//                new LearningDto("key1", "value1", "teacher1"),
//                new LearningDto("key2", "value2", "teacher2")
//        );
//        ApiResponse<?> mockResponse = ApiResponse.success(dtoList);
//
//        doReturn(mockResponse)
//                .when(learningService)
//                .getLearningList(any(CustomUser.class));
//
//        // when / then
//        mockMvc.perform(get("/list")
//                        // @AuthenticationPrincipal CustomUser user 를 주입하기 위한 principal 설정
//                        .with(user(new CustomUser("a", "b"))
//                                // 권한이 필요하면 추가
//                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
//                .andExpect(status().isOk())
//                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
//
//        // 응답 JSON 구조를 확실히 알면 jsonPath로 더 검증 가능:
//        // .andExpect(jsonPath("$.success").value(true))
//        // .andExpect(jsonPath("$.data[0].key").value("key1"));
//    }
}