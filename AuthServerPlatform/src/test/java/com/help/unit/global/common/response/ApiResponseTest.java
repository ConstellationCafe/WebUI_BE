package com.help.global.common.response;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class ApiResponseTest {
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void successResponseUsesTheSharedApiContract() throws Exception {
		ApiResponse<String> response = ApiResponse.success("value");

		JsonNode json = objectMapper.valueToTree(response);

		assertThat(json.path("success").asBoolean()).isTrue();
		assertThat(json.path("response").asText()).isEqualTo("value");
		assertThat(json.has("error")).isTrue();
		assertThat(json.path("error").isNull()).isTrue();
	}

	@Test
	void errorResponseKeepsTheStatusAndOmitsResponseData() throws Exception {
		ApiResponse<?> response = ApiResponse.error("invalid request", HttpStatus.BAD_REQUEST);

		JsonNode json = objectMapper.valueToTree(response);

		assertThat(json.path("success").asBoolean()).isFalse();
		assertThat(json.path("response").isNull()).isTrue();
		assertThat(json.path("error").path("message").asText()).isEqualTo("invalid request");
		assertThat(json.path("error").path("status").asInt())
				.isEqualTo(HttpStatusCode.valueOf(400).value());
	}
}
