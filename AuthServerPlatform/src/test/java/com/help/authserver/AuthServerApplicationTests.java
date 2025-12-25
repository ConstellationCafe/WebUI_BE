package com.help.authserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = AuthServerApplication.class)  // @SpringBootConfiguration 위치
@ActiveProfiles("test")  // application-test.yml 사용
class AuthServerApplicationTests {
	@Test
	void contextLoads() {
		// 스프링 애플리케이션 컨텍스트가 정상적으로 로딩되는지 확인
		// 코드는 불필요, 어플리케이션 실행 성공 여부만 체크
	}
}
