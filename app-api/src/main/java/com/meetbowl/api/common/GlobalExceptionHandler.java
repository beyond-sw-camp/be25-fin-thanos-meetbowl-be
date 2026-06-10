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

/** app-api의 모든 예외를 문서의 공통 실패 응답 포맷으로 변환한다. Controller에서는 try-catch로 응답을 직접 만들지 않는다. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 업무 실패를 HTTP 계층에 모아 Domain/Application이 전송 프로토콜에 의존하지 않게 한다. */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        ErrorCode errorCode = exception.errorCode();
        return ResponseEntity.status(errorCode.httpStatus())
                .body(ApiResponse.fail(errorCode, exception.getMessage(), exception.details()));
    }

    /** 입력 위치와 무관하게 validation 오류가 동일한 외부 계약을 갖도록 변환 책임을 중앙화한다. */
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

    /** Spring의 세부 파싱 예외가 외부 API 계약으로 누출되지 않도록 하나의 요청 오류로 추상화한다. */
    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        MissingServletRequestParameterException.class,
        MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleInvalidRequestException(Exception exception) {
        ErrorCode errorCode = ErrorCode.COMMON_INVALID_REQUEST;
        return ResponseEntity.status(errorCode.httpStatus()).body(ApiResponse.fail(errorCode));
    }

    /** 인가 방식이 바뀌어도 클라이언트의 403 계약이 달라지지 않도록 보안 예외를 경계에서 흡수한다. */
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

    /** 내부 구현 정보와 민감 데이터가 예외 메시지를 통해 노출되지 않도록 마지막 경계를 둔다. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception exception) {
        ErrorCode errorCode = ErrorCode.COMMON_INTERNAL_ERROR;
        return ResponseEntity.status(errorCode.httpStatus()).body(ApiResponse.fail(errorCode));
    }
}
