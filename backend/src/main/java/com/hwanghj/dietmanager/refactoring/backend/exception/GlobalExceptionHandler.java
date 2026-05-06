package com.hwanghj.dietmanager.refactoring.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
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
            .status(HttpStatus.CONFLICT)            // 409 Conflict. 서버 상태와 충돌.
            .body(response);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        ErrorResponse response = ErrorResponse.of("VALIDATION_ERROR", "입력값 검증에 실패했습니다.");
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)         // 400 Bad Request. 요청 자체가 잘못됨.
            .body(response);
    }

    @ExceptionHandler(InvalidLoginException.class)
    public ResponseEntity<ErrorResponse> handleInvalidLoginException(InvalidLoginException e) {
        ErrorResponse response = ErrorResponse.of("INVALID_LOGIN", e.getMessage());

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)            // 401 Unauthorized. 인증 실패 또는 인증 정보 없음.
                .body(response);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFoundException(UserNotFoundException e) {
        ErrorResponse response = ErrorResponse.of("USER_NOT_FOUND", e.getMessage());

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)   // 404 Not Found. 서버는 살아있지만, 요청한 리소스를 찾을 수 없음.
            .body(response);
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPasswordException(InvalidPasswordException e) {
        ErrorResponse response = ErrorResponse.of("INVALID_PASSWORD", e.getMessage());

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST) // 400 Bad Request. 클라이언트의 요청 자체가 잘못됨.
            .body(response);
    }

    @ExceptionHandler(UserProfileNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserProfileNotFoundException(UserProfileNotFoundException e) {
        ErrorResponse response = ErrorResponse.of("USER_PROFILE_NOT_FOUND", e.getMessage());

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)   // 404 Not Found. 서버는 살아있지만, 요청한 리소스를 찾을 수 없음.
            .body(response);
    }

}
