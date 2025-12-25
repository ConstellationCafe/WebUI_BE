package com.help.backend.domain.repository.controller;

import com.help.authserver.AuthServerApplication;
import com.help.authserver.global.jwt.CustomUser;
import com.help.authserver.global.jwt.JwtUtil;
import com.help.backend.domain.repository.repository.LearningRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collection;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = AuthServerApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        // 테스트용 HS256 시크릿 (최소 256bit 이상 권장)
        // "test-test-test-test-test-test-test-test" 를 base64로 인코딩한 값 예시(직접 생성해서 넣는 걸 권장)
        "jwt.secret=dGVzdC10ZXN0LXRlc3QtdGVzdC10ZXN0LXRlc3QtdGVzdC10ZXN0"
})
@Import(LearningListIntegrationTest.TestSecurityConfig.class)
class LearningListIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired LearningRepository repo;

    @Autowired JwtUtil jwtUtil; // HS256 기반 유틸

    private final Long userId = 1L;
    private final String username = "testuser";

//    @BeforeEach
//    void setUp() {
//        repo.deleteAll();
//        repo.save(new Learning(userId, "A"));
//        repo.save(new Learning(userId, "B"));
//        repo.save(new Learning(999L, "OTHER"));
//    }

    @Test
    void list_withoutJwt_401() throws Exception {
        mockMvc.perform(get("/list"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void list_withJwt_200_andReturnsOnlyMyData() throws Exception {
        Collection<? extends GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        CustomUser user = new CustomUser(username, "nick", authorities);

        // JwtUtil.createAccessToken()은 "Bearer " 붙여서 반환함 (너희 코드 기준)
        String bearer = jwtUtil.createAccessToken(user);
        mockMvc.perform(get("/list")
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user").value(username))
                .andExpect(jsonPath("$.count").value(2))
                .andExpect(jsonPath("$.items[0]").exists());
    }

    /**
     * 테스트에서만 Security 정책을 "list는 인증 필요"로 강제.
     * (현재 너희 SecurityConfig는 /** permitAll 이라 401이 안 나올 수 있음)
     *
     * 또한 실제 JwtAuthenticationFilter 빈이 있어야 정상 동작함.
     */
    @TestConfiguration
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain testFilterChain(HttpSecurity http,
                                            com.help.authserver.global.jwt.JwtAuthenticationFilter jwtAuthenticationFilter)
                throws Exception {

            http.csrf(AbstractHttpConfigurer::disable);
            http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

            http.authorizeHttpRequests(auth -> auth
                    .requestMatchers("/list").authenticated()
                    .anyRequest().permitAll()
            );

            // 너희 authserver 코드처럼 필터를 넣되, 순서는 보통 UsernamePasswordAuthenticationFilter 앞이 흔함.
            // 여기서는 기존 방식 유지(ExceptionTranslationFilter 앞).
            http.addFilterBefore(jwtAuthenticationFilter, ExceptionTranslationFilter.class);

            return http.build();
        }
    }
}