package com.meetbowl.common.exception;

/** API 실패 응답의 code와 HTTP status를 한 곳에서 관리한다. 새 도메인 에러가 필요하면 docs/api-spec.md의 에러 계약과 함께 추가한다. */
public enum ErrorCode {
    // Common
    VALIDATION_FAILED("VALIDATION_FAILED", "요청 값이 올바르지 않습니다.", 400),
    COMMON_INVALID_REQUEST("COMMON_INVALID_REQUEST", "잘못된 요청입니다.", 400),
    COMMON_UNAUTHORIZED("COMMON_UNAUTHORIZED", "인증이 필요합니다.", 401),
    COMMON_FORBIDDEN("COMMON_FORBIDDEN", "권한이 없습니다.", 403),
    COMMON_NOT_FOUND("COMMON_NOT_FOUND", "리소스를 찾을 수 없습니다.", 404),
    COMMON_CONFLICT("COMMON_CONFLICT", "요청 상태가 충돌합니다.", 409),
    COMMON_INTERNAL_ERROR("COMMON_INTERNAL_ERROR", "서버 내부 오류가 발생했습니다.", 500),

    // Auth
    AUTH_INVALID_CREDENTIALS("AUTH_INVALID_CREDENTIALS", "로그인 정보가 올바르지 않습니다.", 401),
    AUTH_TOKEN_EXPIRED("AUTH_TOKEN_EXPIRED", "토큰이 만료되었습니다.", 401),
    AUTH_INITIAL_PASSWORD_CHANGE_REQUIRED(
            "AUTH_INITIAL_PASSWORD_CHANGE_REQUIRED", "초기 비밀번호 변경이 필요합니다.", 403),

    // User
    USER_NOT_FOUND("USER_NOT_FOUND", "사용자를 찾을 수 없습니다.", 404),

    // Meeting
    MEETING_NOT_FOUND("MEETING_NOT_FOUND", "회의를 찾을 수 없습니다.", 404),
    MEETING_ROOM_ALREADY_RESERVED("MEETING_ROOM_ALREADY_RESERVED", "회의실이 이미 예약되어 있습니다.", 409),
    MEETING_FORBIDDEN_GUEST_ACCESS(
            "MEETING_FORBIDDEN_GUEST_ACCESS", "게스트가 접근할 수 없는 회의 기능입니다.", 403),

    // Minutes
    MINUTES_NOT_FOUND("MINUTES_NOT_FOUND", "회의록을 찾을 수 없습니다.", 404),
    MINUTES_REVIEW_REQUIRED("MINUTES_REVIEW_REQUIRED", "회의록 검토자 승인이 필요합니다.", 409),
    MINUTES_ALREADY_APPROVED("MINUTES_ALREADY_APPROVED", "이미 승인된 회의록입니다.", 409),

    // Mail
    MAIL_FORBIDDEN_ACCESS("MAIL_FORBIDDEN_ACCESS", "메일 접근 권한이 없습니다.", 403),

    // File
    FILE_INVALID_EXTENSION("FILE_INVALID_EXTENSION", "허용되지 않는 파일 형식입니다.", 415),
    FILE_SIZE_EXCEEDED("FILE_SIZE_EXCEEDED", "파일 크기 제한을 초과했습니다.", 413),

    // AI/STT integration
    AI_RAG_ACCESS_DENIED("AI_RAG_ACCESS_DENIED", "AI 자료 접근 권한이 없습니다.", 403),
    AI_RESPONSE_VALIDATION_FAILED("AI_RESPONSE_VALIDATION_FAILED", "AI 응답 값이 올바르지 않습니다.", 502),

    STT_SESSION_NOT_FOUND("STT_SESSION_NOT_FOUND", "STT 세션을 찾을 수 없습니다.", 404),
    STT_PROVIDER_UNAVAILABLE("STT_PROVIDER_UNAVAILABLE", "STT Provider를 사용할 수 없습니다.", 503),
    STT_TRANSCRIPT_PUBLISH_FAILED(
            "STT_TRANSCRIPT_PUBLISH_FAILED", "STT Transcript 발행에 실패했습니다.", 502);

    private final String code;
    private final String message;
    private final int httpStatus;

    ErrorCode(String code, String message, int httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }

    public int httpStatus() {
        return httpStatus;
    }
}
