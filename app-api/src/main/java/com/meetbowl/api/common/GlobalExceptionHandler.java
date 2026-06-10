package com.meetbowl.api.common;

import java.util.List;

import jakarta.validation.ConstraintViolationException;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.common.response.ApiResponse;
import com.meetbowl.common.response.ErrorDetail;

/**
 * 예외와 HTTP 응답의 매핑을 이 경계에 모아 Controller가 전송 계층의 실패 형식을 결정하지 못하게 한다.
 * 그래야 같은 업무 오류가 어느 API에서 발생하더라도 상태 코드와 응답 계약이 달라지지 않는다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        ErrorCode errorCode = exception.errorCode();
        return ResponseEntity.status(errorCode.httpStatus())
                .body(ApiResponse.fail(errorCode, exception.getMessage(), exception.details()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException exception) {
        List<ErrorDetail> details =
                exception.getBindingResult().getFieldErrors().stream()
                        .map(error -> new ErrorDetail(error.getField(), error.getDefaultMessage()))
                        .toList();

        ErrorCode errorCode = ErrorCode.VALIDATION_FAILED;
        return ResponseEntity.status(errorCode.httpStatus())
                .body(ApiResponse.fail(errorCode, details));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(BindException exception) {
        List<ErrorDetail> details =
                exception.getBindingResult().getFieldErrors().stream()
                        .map(error -> new ErrorDetail(error.getField(), error.getDefaultMessage()))
                        .toList();

        ErrorCode errorCode = ErrorCode.VALIDATION_FAILED;
        return ResponseEntity.status(errorCode.httpStatus())
                .body(ApiResponse.fail(errorCode, details));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(
            ConstraintViolationException exception) {
        List<ErrorDetail> details =
                exception.getConstraintViolations().stream()
                        .map(
                                violation ->
                                        new ErrorDetail(
                                                violation.getPropertyPath().toString(),
                                                violation.getMessage()))
                        .toList();

        ErrorCode errorCode = ErrorCode.VALIDATION_FAILED;
        return ResponseEntity.status(errorCode.httpStatus())
                .body(ApiResponse.fail(errorCode, details));
    }

    // 프레임워크별 파싱 예외 이름이 외부 API의 오류 코드를 결정하지 않도록 하나의 계약으로 수렴시킨다.
    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        MissingServletRequestParameterException.class,
        MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleInvalidRequestException(Exception exception) {
        ErrorCode errorCode = ErrorCode.COMMON_INVALID_REQUEST;
        return ResponseEntity.status(errorCode.httpStatus()).body(ApiResponse.fail(errorCode));
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthorizationDeniedException(
            AuthorizationDeniedException exception) {
        ErrorCode errorCode = ErrorCode.COMMON_FORBIDDEN;
        return ResponseEntity.status(errorCode.httpStatus()).body(ApiResponse.fail(errorCode));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(
            NoResourceFoundException exception) {
        ErrorCode errorCode = ErrorCode.COMMON_NOT_FOUND;
        return ResponseEntity.status(errorCode.httpStatus()).body(ApiResponse.fail(errorCode));
    }

    // 처리되지 않은 예외의 메시지에는 구현 정보나 민감값이 포함될 수 있어 고정된 오류만 노출한다.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception exception) {
        ErrorCode errorCode = ErrorCode.COMMON_INTERNAL_ERROR;
        return ResponseEntity.status(errorCode.httpStatus()).body(ApiResponse.fail(errorCode));
    }
}
