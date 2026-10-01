package com.help.erpweb.domain.notification.controller;

import com.help.erpweb.domain.notification.exception.InvalidNotificationException;
import com.help.erpweb.domain.notification.exception.NotificationRequestConflictException;
import com.help.erpweb.domain.notification.exception.NotificationScopeDeniedException;
import com.help.erpweb.domain.notification.exception.NotificationStreamUnavailableException;
import com.help.erpweb.domain.notification.exception.NotificationTargetNotFoundException;
import com.help.global.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 알림 API 전용 오류 변환. 공통 {@code ApiResponse(success, response, error)} 규격을 따르며,
 * 공통 핸들러보다 먼저 적용되어 알림 도메인 오류가 500으로 떨어지지 않게 한다.
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackageClasses = NotificationExceptionHandler.class)
public class NotificationExceptionHandler {

    @ExceptionHandler(InvalidNotificationException.class)
    public ResponseEntity<ApiResponse<?>> invalid(InvalidNotificationException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<?>> unreadable(HttpMessageNotReadableException ex) {
        return error(HttpStatus.BAD_REQUEST, "요청 JSON 형식이나 허용되지 않은 값을 확인해 주세요.");
    }

    @ExceptionHandler(NotificationScopeDeniedException.class)
    public ResponseEntity<ApiResponse<?>> scopeDenied(NotificationScopeDeniedException ex) {
        return error(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(NotificationTargetNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> targetNotFound(NotificationTargetNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(NotificationRequestConflictException.class)
    public ResponseEntity<ApiResponse<?>> conflict(NotificationRequestConflictException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(NotificationStreamUnavailableException.class)
    public ResponseEntity<ApiResponse<?>> streamUnavailable(NotificationStreamUnavailableException ex) {
        log.warn("실시간 알림 연결 거부 - reason=capacity_or_shutdown");
        return error(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    private static ResponseEntity<ApiResponse<?>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ApiResponse.error(message, status));
    }
}
