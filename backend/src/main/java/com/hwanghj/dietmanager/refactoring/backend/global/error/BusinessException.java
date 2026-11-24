package com.hwanghj.dietmanager.refactoring.backend.global.error;

/**
 * 업무 규칙 위반을 나타내는 예외의 공통 부모.
 * GlobalExceptionHandler가 errorCode의 HTTP 상태와 코드명으로 응답한다.
 */
public abstract class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    protected BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode.getMessage());
    }

    protected BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
