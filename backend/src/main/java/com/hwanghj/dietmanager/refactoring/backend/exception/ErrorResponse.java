package com.hwanghj.dietmanager.refactoring.backend.exception;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_EMPTY) // errors가 비어 있으면 응답에서 뺀다.
public class ErrorResponse {
    private final String code;
    private final String message;
    private final List<FieldError> errors;

    /** 입력 검증에 실패한 항목. field는 요청 필드명 또는 파라미터명. */
    public record FieldError(String field, String message) {}

    public ErrorResponse(String code, String message, List<FieldError> errors) {
        this.code = code;
        this.message = message;
        this.errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, List.of());
    }

    public static ErrorResponse of(ErrorCode errorCode) {
        return of(errorCode.name(), errorCode.getMessage());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message, List<FieldError> errors) {
        return new ErrorResponse(errorCode.name(), message, errors);
    }
}
