package com.help.erpweb.naming;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.help.erpweb.domain.modules.academy.dto.request.LessonRecordCreateRequest;
import com.help.erpweb.domain.modules.academy.dto.response.OptionResponse;
import com.help.erpweb.domain.modules.chatbot.content.dto.request.repository.ContentDto;
import com.help.erpweb.domain.modules.chatbot.learning.dto.request.repository.LearningDto;
import com.help.erpweb.domain.modules.competition.dto.response.CompetitionWinnerResponse;
import com.help.erpweb.domain.modules.erp.penalty.dto.response.PenaltyMemberRankResponse;
import org.junit.jupiter.api.Test;

/**
 * ADR-0008: 새 필드 이름으로 바꾸는 동안 FE가 이전 이름으로도 읽고 보낼 수 있어야 한다.
 */
class LegacyFieldNameCompatibilityTest {

    private final ObjectMapper mapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();

    @Test
    void academyOptionWritesNewAndLegacyNames() {
        JsonNode json = mapper.valueToTree(new OptionResponse("sk-1", "123", "별"));

        assertThat(json.get("discordId").asText()).isEqualTo("123");
        assertThat(json.get("displayName").asText()).isEqualTo("별");
        assertThat(json.get("discordID").asText()).isEqualTo("123");
        assertThat(json.get("name").asText()).isEqualTo("별");
    }

    @Test
    void competitionWinnerAndPenaltyRankWriteLegacyDisplayNames() {
        JsonNode winner = mapper.valueToTree(
                new CompetitionWinnerResponse("대회", "s1", "123", "별", java.time.LocalDate.of(2026, 10, 2)));
        assertThat(winner.get("winnerDisplayName").asText()).isEqualTo("별");
        assertThat(winner.get("winnerName").asText()).isEqualTo("별");

        JsonNode rank = mapper.valueToTree(new PenaltyMemberRankResponse("123", "별", 1L, 1L, null));
        assertThat(rank.get("displayName").asText()).isEqualTo("별");
        assertThat(rank.get("username").asText()).isEqualTo("별");
    }

    @Test
    void chatbotDtosAcceptLegacyRequestNamesAndWriteBoth() throws Exception {
        ContentDto content = mapper.readValue("{\"cnValue\":\"v\",\"recommender\":\"123\"}", ContentDto.class);
        assertThat(content.getRecommenderDiscordId()).isEqualTo("123");

        JsonNode written = mapper.valueToTree(content);
        assertThat(written.get("recommenderDiscordId").asText()).isEqualTo("123");
        assertThat(written.get("recommender").asText()).isEqualTo("123");

        LearningDto learning = mapper.readValue(
                "{\"lnKey\":\"k\",\"lnValue\":\"v\",\"teacherDiscordId\":\"456\"}", LearningDto.class);
        assertThat(learning.getTeacherDiscordId()).isEqualTo("456");
        assertThat(mapper.valueToTree(learning).get("teacher").asText()).isEqualTo("456");
    }

    @Test
    void lessonRecordCreateRequestAcceptsLegacyIdNames() throws Exception {
        LessonRecordCreateRequest request = mapper.readValue("""
                {"academyId":1,"className":"1","subject":"1","mainTeacherId":"sk-t",
                 "coTeacherIds":["sk-c"],"memberIds":["sk-m"],"description":""}
                """, LessonRecordCreateRequest.class);

        assertThat(request.mainTeacherSk()).isEqualTo("sk-t");
        assertThat(request.coTeacherSks()).isEqualTo(List.of("sk-c"));
        assertThat(request.memberSks()).isEqualTo(List.of("sk-m"));
    }
}
