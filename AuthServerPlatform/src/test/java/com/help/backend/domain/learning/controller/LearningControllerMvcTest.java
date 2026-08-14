//package com.help.backend.domain.learning.controller;
//
//import static org.hamcrest.Matchers.not;
//import static org.hamcrest.Matchers.emptyOrNullString;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.doReturn;
//import static org.mockito.Mockito.verify;
//import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import com.help.authserver.domain.user.entity.constellation.DiscordUser;
//import com.help.backend.domain.learning.dto.request.repository.LearningDto;
//import com.help.backend.domain.learning.service.LearningService;
//import com.help.global.common.response.ApiResponse;
//import com.help.global.jwt.AuthServerJwtAuthFilter;
//import com.help.global.jwt.BackEndJwtAuthFilter;
//import com.help.global.jwt.CustomUser;
//import com.help.global.jwt.JwtUtil;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.context.annotation.ComponentScan;
//import org.springframework.context.annotation.FilterType;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.servlet.MockMvc;
//
//@WebMvcTest(
//    controllers = LearningController.class,
//    excludeFilters = @ComponentScan.Filter(
//        type = FilterType.ASSIGNABLE_TYPE,
//        classes = {AuthServerJwtAuthFilter.class, BackEndJwtAuthFilter.class}
//    )
//)
//@AutoConfigureMockMvc
//class LearningControllerMvcTest {
//    @Autowired MockMvc mockMvc;
//    @MockitoBean
//    JwtUtil jwtUtil;
//
//    @MockitoBean
//    LearningService learningService;
//
//    @Test
//    void getLearningListTest() throws Exception {
//        // given
//        List<String> authorities = new ArrayList<>();
//        authorities.add("서버장");
//        DiscordUser discordUser = DiscordUser.of("434719628875399169", authorities);
//        CustomUser customUser = CustomUser.from(discordUser);
//
//        List<LearningDto> dtoList = List.of(
//                new LearningDto("key1", "value1", "1234"),
//                new LearningDto("key2", "value2", "5618")
//        );
//        ApiResponse<?> mockResponse = ApiResponse.success(dtoList);
//
//        // <?> 타입 추론 문제로 인해 미사용
////        Mockito.when(learningService.getLearningList(user))
////               .thenReturn(mockResponse);
//
//        doReturn(mockResponse)
//                .when(learningService)
//                .getLearningList(any(CustomUser.class));
//
//        mockMvc.perform(get("/api/repository/learning/list")
//                        .with(user(customUser))
//                        .accept(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk())
//                .andExpect(content().string(not(emptyOrNullString())))
//                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
//                .andExpect(jsonPath("$.success").value(true))
//                // 첫 번째 요소
//                .andExpect(jsonPath("$.response[0].lnKey").value("key1"))
//                .andExpect(jsonPath("$.response[0].lnValue").value("value1"))
//                .andExpect(jsonPath("$.response[0].teacher").value("1234"))
//                // 두 번째 요소
//                .andExpect(jsonPath("$.response[1].lnKey").value("key2"))
//                .andExpect(jsonPath("$.response[1].lnValue").value("value2"))
//                .andExpect(jsonPath("$.response[1].teacher").value("5618"));
//
//        verify(learningService).getLearningList(any(CustomUser.class));
//    }
//}