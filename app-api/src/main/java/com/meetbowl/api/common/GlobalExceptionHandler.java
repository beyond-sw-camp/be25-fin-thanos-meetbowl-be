package com.meetbowl.api.common;

import java.util.List;

import jakarta.validation.ConstraintViolationException;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
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

    /** UseCase와 Domain에서 발생시킨 의도된 업무 실패를 그대로 클라이언트 계약으로 변환한다. */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        ErrorCode errorCode = exception.errorCode();
        return ResponseEntity.status(errorCode.httpStatus())
                .body(ApiResponse.fail(errorCode, exception.getMessage(), exception.details()));
    }

    /**
     * @RequestBody DTO의 @Valid 실패를 field/reason 목록으로 내려준다.
     */
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

    /** Query parameter, form binding 등 body 외 입력값 검증 실패를 처리한다. */
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

    /**
     * @Validated가 붙은 컨트롤러 메서드 파라미터 검증 실패를 처리한다.
     */
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

    /** JSON 파싱 오류, 필수 파라미터 누락, 타입 변환 실패는 모두 잘못된 요청으로 통일한다. */
    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        MissingServletRequestParameterException.class,
        MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleInvalidRequestException(Exception exception) {
        ErrorCode errorCode = ErrorCode.COMMON_INVALID_REQUEST;
        return ResponseEntity.status(errorCode.httpStatus()).body(ApiResponse.fail(errorCode));
    }

    /** 만료·폐기·위조된 JWT는 공통 401 응답으로 변환한다. */
    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ApiResponse<Void>> handleJwtException(JwtException exception) {
        ErrorCode errorCode = ErrorCode.COMMON_UNAUTHORIZED;
        return ResponseEntity.status(errorCode.httpStatus()).body(ApiResponse.fail(errorCode));
    }

    /**
     * @PreAuthorize 같은 메서드 보안 실패를 공통 403 응답으로 변환한다.
     */
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthorizationDeniedException(
            AuthorizationDeniedException exception) {
        ErrorCode errorCode = ErrorCode.COMMON_FORBIDDEN;
        return ResponseEntity.status(errorCode.httpStatus()).body(ApiResponse.fail(errorCode));
    }

    /** 매핑되지 않은 URL 또는 정적 리소스 요청은 공통 404 응답으로 변환한다. */
    @ExceptionHandler({
        NoResourceFoundException.class,
        HttpRequestMethodNotSupportedException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(Exception exception) {
        ErrorCode errorCode = ErrorCode.COMMON_NOT_FOUND;
        return ResponseEntity.status(errorCode.httpStatus()).body(ApiResponse.fail(errorCode));
    }

    /** 예상하지 못한 예외의 내부 상세는 클라이언트에 노출하지 않는다. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception exception) {
        ErrorCode errorCode = ErrorCode.COMMON_INTERNAL_ERROR;
        return ResponseEntity.status(errorCode.httpStatus()).body(ApiResponse.fail(errorCode));
    }
}
