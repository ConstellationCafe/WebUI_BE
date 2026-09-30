package com.help.erpweb.domain.modules.competition.controller;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.help.erpweb.domain.modules.competition.exception.CompetitionConfigNotFoundException;
import com.help.erpweb.domain.modules.competition.exception.CompetitionPostFailedException;
import com.help.erpweb.domain.modules.competition.exception.InvalidCompetitionNoticeException;
import com.help.global.common.response.ApiResponse;

/**
 * 대회 공지 API 전용 오류 변환. 공통 {@code ApiResponse(success, response, error)} 규격을 따르며,
 * 공통 핸들러보다 먼저 적용되어 도메인 오류가 500으로 떨어지지 않게 한다.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackageClasses = CompetitionExceptionHandler.class)
public class CompetitionExceptionHandler {

	@ExceptionHandler(InvalidCompetitionNoticeException.class)
	public ResponseEntity<ApiResponse<?>> invalid(InvalidCompetitionNoticeException ex) {
		return error(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiResponse<?>> unreadable(HttpMessageNotReadableException ex) {
		return error(HttpStatus.BAD_REQUEST, "요청 JSON 형식이나 날짜 형식(예: 2026-10-02T22:00)을 확인해 주세요.");
	}

	@ExceptionHandler(CompetitionConfigNotFoundException.class)
	public ResponseEntity<ApiResponse<?>> configNotFound(CompetitionConfigNotFoundException ex) {
		return error(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(CompetitionPostFailedException.class)
	public ResponseEntity<ApiResponse<?>> postFailed(CompetitionPostFailedException ex) {
		return error(HttpStatus.BAD_GATEWAY, ex.getMessage());
	}

	private static ResponseEntity<ApiResponse<?>> error(HttpStatus status, String message) {
		return ResponseEntity.status(status).body(ApiResponse.error(message, status));
	}
}
