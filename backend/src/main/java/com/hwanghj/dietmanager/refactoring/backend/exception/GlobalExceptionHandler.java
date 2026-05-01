package com.hwanghj.dietmanager.refactoring.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 모든 REST 컨트롤러에서 발생하는 예외를 한 곳에서 공통 처리.
 * 
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmailException(DuplicateEmailException e) {
        // getMessage()는 Throwable에서 상속된 예외 메시지를 반환함.
        ErrorResponse response = ErrorResponse.of("DUPLICATE_EMAIL", e.getMessage());
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(response);
    }   
}
