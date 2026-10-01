package com.help.global.integration;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * WebUI 밖의 시스템(예: Discord 봇)이 서비스 API Key로 호출할 때의 client 목록.
 * <p>
 * 원본 API Key는 저장소와 설정 어디에도 두지 않고 SHA-256 hex만 주입한다. client를
 * 하나도 설정하지 않으면 외부 API는 모든 요청을 401로 거부한다(기본 비활성).
 * 환경 변수 예: {@code INTEGRATION_CLIENTS_0_ID}, {@code INTEGRATION_CLIENTS_0_KEYSHA256},
 * {@code INTEGRATION_CLIENTS_0_BOTIDS}(쉼표 구분). 잘못된 값이면 기동 시 실패한다.
 */
@ConfigurationProperties(prefix = "integration")
public record IntegrationClientProperties(List<Client> clients) {
    public IntegrationClientProperties {
        clients = clients == null ? List.of() : List.copyOf(clients);
    }

    /**
     * @param id        로그·metric·발행 이력에 남는 client 식별자(비밀 아님)
     * @param keySha256 API Key의 SHA-256 hex(소문자 64자)
     * @param botIds    이 client가 알림을 발행할 수 있는 채팅방(botId) 목록
     */
    public record Client(String id, String keySha256, List<String> botIds) {
        private static final Pattern ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");
        private static final Pattern SHA256_HEX = Pattern.compile("[0-9a-f]{64}");

        public Client {
            if (id == null || !ID.matcher(id).matches()) {
                throw new IllegalArgumentException("integration client id 형식이 올바르지 않습니다.");
            }
            if (keySha256 == null || !SHA256_HEX.matcher(keySha256).matches()) {
                throw new IllegalArgumentException(
                        "integration client '" + id + "'의 keySha256은 소문자 SHA-256 hex여야 합니다."
                );
            }
            if (botIds == null || botIds.isEmpty()) {
                throw new IllegalArgumentException(
                        "integration client '" + id + "'에 허용할 botIds를 지정해야 합니다."
                );
            }
            botIds = botIds.stream().map(String::trim).filter(botId -> !botId.isEmpty()).toList();
        }

        public Set<String> botIdSet() {
            return Set.copyOf(botIds);
        }
    }
}
