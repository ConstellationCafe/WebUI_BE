//package com.help.authserver.domain.user.service;
//
//import com.help.authserver.api.LoginAPI;
//import com.help.authserver.domain.user.dto.response.LoginResponseDto;
//import com.help.authserver.domain.user.dto.user.DiscordUserDto;
//import com.help.authserver.domain.user.entity.constellation.DiscordUser;
//import com.help.authserver.domain.user.repository.constellation.DiscordUserRepository;
//import com.help.global.common.response.ApiResponse;
//import com.help.global.jwt.CustomUser;
//import com.help.global.jwt.JwtUtil;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.Test;
//import org.mockito.Mockito;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.http.ResponseCookie;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//import jakarta.servlet.http.HttpServletResponse;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Optional;
//
//@SpringBootTest
//public class DiscordAuthServiceTest {
//    @MockitoBean
//    JwtUtil jwtUtil;
//    @MockitoBean
//    LoginAPI<DiscordUserDto> loginAPI;
//    @MockitoBean
//    DiscordUserRepository userRepository;
//    @Autowired
//    DiscordAuthService authService;
//
//    static String testDiscordID = "434719628875399169";
//    static String testUsername = "재의 마녀 일레이나";
//
//    @Test
//    void loginTest() {
//        // given
//        String code = "test-code";
//        String redirectUrl = "http://localhost/redirect";
//
//        // LoginAPI Mock
//        DiscordUserDto discordUserDto = new DiscordUserDto(testDiscordID, testUsername, "avatar");
//        Mockito.when(loginAPI.getUserInfo(code))
//               .thenReturn(discordUserDto);
//
//        // DiscordUserRepository Mock
//        List<String> authorities = new ArrayList<>();
//        authorities.add("서버장");
//        DiscordUser user = DiscordUser.of(discordUserDto.discordId(), authorities);
//        Mockito.when(userRepository.findByDiscordID(discordUserDto.discordId()))
//                .thenReturn(Optional.of(user));
//
//        // JwtUtil Mock
//        Mockito.when(jwtUtil.createAccessToken(Mockito.any(CustomUser.class)))
//                .thenReturn("access-token");
//        Mockito.when(jwtUtil.createRefreshToken(Mockito.any(CustomUser.class)))
//                .thenReturn("refresh-token");
//        ResponseCookie refreshCookie = ResponseCookie
//                .from("refreshToken", "refresh-token")
//                .path("/")
//                .httpOnly(true)
//                .build();
//        Mockito.when(jwtUtil.createRefreshTokenCookie("refresh-token"))
//                .thenReturn(refreshCookie);
//
//        // when
//        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
//        String result = authService.login(code, response);
//
//        // then
////        LoginResponseDto dto = (LoginResponseDto) result.getResponse();
////        org.junit.jupiter.api.Assertions.assertEquals("access-token", dto.accessToken());
////        org.junit.jupiter.api.Assertions.assertEquals(redirectUrl, dto.redirectUrl());
////
////        Mockito.verify(loginAPI).getUserInfo(code);
////        Mockito.verify(response).addHeader("Authorization", "access-token");
////        Mockito.verify(response).addHeader("Set-Cookie", refreshCookie.toString());
//    }
//}
