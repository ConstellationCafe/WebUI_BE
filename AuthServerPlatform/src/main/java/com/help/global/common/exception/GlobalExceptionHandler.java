package com.help.global.common.exception;

import java.util.List;

import com.help.global.common.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
	@ExceptionHandler(UsernameNotFoundException.class)
	public ResponseEntity<ApiResponse<?>> handleUsernameNotFoundException(final UsernameNotFoundException ex) {
		log.error("UsernameNotFoundException : {}", ex.getMessage());
		return ResponseEntity
			.status(ErrorCode.INVALID_CREDENTIALS.getStatus())
			.body(ApiResponse.error(ErrorCode.INVALID_CREDENTIALS));
	}

	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<ApiResponse<?>> handleBadCredentialsException(final BadCredentialsException ex) {
		log.error("BadCredentialsException : {}", ex.getMessage());
		return ResponseEntity
			.status(ErrorCode.INVALID_CREDENTIALS.getStatus())
			.body(ApiResponse.error(ErrorCode.INVALID_CREDENTIALS));
	}

	@ExceptionHandler(NoHandlerFoundException.class)
	public ResponseEntity<ApiResponse<?>> handleNoHandlerFoundException(final NoHandlerFoundException ex) {
		log.error("NoHandlerFoundException : {}", ex.getMessage());
		return ResponseEntity
			.status(ErrorCode.NOT_FOUND.getStatus())
			.body(ApiResponse.error(ErrorCode.NOT_FOUND));
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ApiResponse<?>> handleHttpRequestMethodNotSupportedException(
		final HttpRequestMethodNotSupportedException ex
	) {
		log.error("HttpRequestMethodNotSupportedException : {}", ex.getMessage());
		return ResponseEntity
			.status(ErrorCode.NOT_FOUND.getStatus())
			.body(ApiResponse.error(ErrorCode.NOT_FOUND));
	}

	@ExceptionHandler(AuthorizationDeniedException.class)
	public ResponseEntity<ApiResponse<?>> handleAuthorizationDeniedException(final AuthorizationDeniedException ex) {
		final ErrorCode errorCode;
		final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (null == authentication || authentication instanceof AnonymousAuthenticationToken) {
			errorCode = ErrorCode.UNAUTHORIZED;
		} else {
			errorCode = ErrorCode.NOT_FOUND;
		}
		log.error("Access Denied : {}", errorCode.getMessage());

		return ResponseEntity
			.status(errorCode.getStatus())
			.body(ApiResponse.error(errorCode));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<?>> handleValidationException(final MethodArgumentNotValidException ex) {
		log.error("MethodArgumentNotValidException : {}", ex.getMessage());
		final BindingResult bindingResult = ex.getBindingResult();
		final List<FieldError> fieldErrors = bindingResult.getFieldErrors();

		final String errorMessage = fieldErrors.stream()
			.findFirst()
			.map(FieldError::getDefaultMessage)
			.orElse("Validation error");

		return ResponseEntity
			.status(HttpStatus.BAD_REQUEST)
			.body(ApiResponse.error(errorMessage, HttpStatus.BAD_REQUEST));
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiResponse<?>> handleConstraintViolationException(final ConstraintViolationException ex) {
		log.error("ConstraintViolationException : {}", ex.getMessage());
		final String errorMessage = ex.getConstraintViolations().stream()
			.findFirst()
			.map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
			.orElse("Validation error");

		return ResponseEntity
			.status(HttpStatus.BAD_REQUEST)
			.body(ApiResponse.error(errorMessage, HttpStatus.BAD_REQUEST));
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiResponse<?>> handleMethodArgumentTypeMismatchException(
		final MethodArgumentTypeMismatchException ex
	) {
		log.error("MethodArgumentTypeMismatchException : {}", ex.getMessage());
		final String name = ex.getName();
		final String message = String.format("%s의 형식이 잘못되었습니다", name);

		return ResponseEntity
			.status(HttpStatus.BAD_REQUEST)
			.body(ApiResponse.error(message, HttpStatus.BAD_REQUEST));
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ApiResponse<?>> handleMissingParams(final MissingServletRequestParameterException ex) {
		log.error("MissingServletRequestParameterException : {}", ex.getMessage());
		final String name = ex.getParameterName();
		final String message = String.format("필수 요청 파라미터 '%s'가 누락되었습니다", name);

		return ResponseEntity
			.status(HttpStatus.BAD_REQUEST)
			.body(ApiResponse.error(message, HttpStatus.BAD_REQUEST));
	}
	
	@ExceptionHandler(CustomException.class)
	public ResponseEntity<ApiResponse<?>> handleCustomException(final CustomException ex) {
		log.error("CustomException : {}", ex.getMessage());
		final ErrorCode errorCode = ex.getErrorCode();
		return ResponseEntity
			.status(errorCode.getStatus())
			.body(ApiResponse.error(errorCode));
	}

	@ExceptionHandler(RuntimeException.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public ApiResponse<?> unknownServerError(final RuntimeException ex) {
		log.error("서버 오류 : {}", ex.getMessage());
		return ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR);
	}
}
