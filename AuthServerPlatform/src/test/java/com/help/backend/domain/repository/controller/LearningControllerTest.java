package com.help.backend.domain.repository.controller;

import java.util.List;

import com.help.authserver.global.jwt.JwtUtil;
import com.help.backend.domain.repository.dto.request.repository.LearningDto;
import com.help.backend.domain.repository.service.LearningService;
import com.help.backend.global.common.response.ApiResponse;
import com.help.backend.global.jwt.CustomUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(MockitoExtension.class)
class LearningControllerTest {

    @Mock
    private LearningService learningService;

    // 컨트롤러가 JwtUtil을 직접 쓰는 구조라면 유지, 아니면 제거해도 됨
    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private LearningController controller;

    @Test
    void getLearningList_returnsOk_andBody() {
        // given
        CustomUser user = Mockito.mock(CustomUser.class);

        List<LearningDto> dtoList = List.of(
                new LearningDto("key1", "value1", "teacher1"),
                new LearningDto("key2", "value2", "teacher2")
        );
        ApiResponse<?> mockResponse = ApiResponse.success(dtoList);

        Mockito.doReturn(mockResponse)
                .when(learningService)
                .getLearningList(Mockito.any(CustomUser.class));

        // when
        ApiResponse<?> res = controller.getLearningList(user);

        // then
        assertThat(res).isNotNull();
        assertThat(res.isSuccess()).isTrue();

//        @SuppressWarnings("unchecked")
//        List<LearningDto> body = (List<LearningDto>) res.getResponse();
//        assertThat(body).hasSize(2);
//        assertThat(body.get(0).getLnKey()).isEqualTo("key1");
//
//        then(learningService).should().getLearningList(Mockito.any(CustomUser.class));
    }
}