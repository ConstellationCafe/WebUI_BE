package com.help.global.integration;

import java.util.Set;

/**
 * API Key로 인증된 외부 시스템. SecurityContext의 principal로 들어간다.
 */
public record IntegrationClient(String id, Set<String> botIds) {
    public boolean canPublishTo(String botId) {
        return botIds.contains(botId);
    }

    @Override
    public String toString() {
        return "IntegrationClient[" + id + "]";
    }
}
