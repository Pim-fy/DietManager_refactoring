package com.hwanghj.dietmanager.refactoring.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * 클라이언트에 내려주는 오류 코드 목록.
 * 코드명, HTTP 상태, 기본 메시지를 한 곳에서 관리한다.
 * 코드명은 프론트와의 계약이므로 바꿀 때는 프론트 사용처를 함께 확인한다.
 */
public enum ErrorCode {
    // 공통
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "입력값을 확인해주세요."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "허용되지 않은 요청입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 주소를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 요청 방식입니다."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 요청 형식입니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "요청을 처리하지 못했습니다. 잠시 후 다시 시도해주세요."),

    // 사용자·인증
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    INVALID_LOGIN(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 일치하지 않습니다."),
    SESSION_EXPIRED(HttpStatus.UNAUTHORIZED, "로그인이 만료되었습니다. 다시 로그인해주세요."),
    LOGIN_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "로그인 실패가 반복되어 잠시 제한되었습니다. 10분 후 다시 시도해주세요.", 600),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "현재 비밀번호가 일치하지 않습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    USER_PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자 프로필을 찾을 수 없습니다."),

    // 신체 기록
    BODY_MEASUREMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "신체 측정값을 찾을 수 없습니다."),
    BODY_MEASUREMENT_CONFLICT(HttpStatus.CONFLICT, "신체 기록을 변경할 수 없는 상태입니다."),

    // 식품
    FOOD_NOT_FOUND(HttpStatus.NOT_FOUND, "저장된 식품을 찾을 수 없습니다."),
    FOOD_SELECTION_CONFLICT(HttpStatus.CONFLICT, "선택한 식품을 사용할 수 없습니다."),
    FOOD_API_ERROR(HttpStatus.BAD_GATEWAY, "식품 API 요청을 처리하지 못했습니다. 잠시 후 다시 시도해주세요."),
    FOOD_API_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "식품 API 호출 한도에 도달했습니다. 잠시 후 다시 시도해주세요.", 60),

    // 섭취 기록
    INTAKE_NOT_FOUND(HttpStatus.NOT_FOUND, "섭취 기록을 찾을 수 없습니다."),
    INTAKE_CONFLICT(HttpStatus.CONFLICT, "섭취 기록을 변경할 수 없는 상태입니다."),

    // 식단 계획
    MEAL_PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "식단 계획을 찾을 수 없습니다."),
    MEAL_PLAN_CONFLICT(HttpStatus.CONFLICT, "식단 계획을 변경할 수 없는 상태입니다.");

    private final HttpStatus status;
    private final String message;
    private final int retryAfterSeconds; // 0이면 Retry-After 헤더를 보내지 않는다.

    ErrorCode(HttpStatus status, String message) {
        this(status, message, 0);
    }

    ErrorCode(HttpStatus status, String message, int retryAfterSeconds) {
        this.status = status;
        this.message = message;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
