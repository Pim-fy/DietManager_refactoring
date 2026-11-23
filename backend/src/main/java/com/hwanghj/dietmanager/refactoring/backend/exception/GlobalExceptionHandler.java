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
    @ExceptionHandler(com.hwanghj.dietmanager.refactoring.backend.statistics.StatisticsRangeException.class)
    public ResponseEntity<ErrorResponse> handleStatisticsRange(RuntimeException e) {
        return ResponseEntity.badRequest().body(ErrorResponse.of("VALIDATION_ERROR", e.getMessage()));
    }

    @ExceptionHandler(com.hwanghj.dietmanager.refactoring.backend.security.LoginAttemptLimiter.Limited.class)
    public ResponseEntity<ErrorResponse> handleLoginLimit(RuntimeException e) {
        return ResponseEntity.status(429).header("Retry-After", "600")
            .body(ErrorResponse.of("LOGIN_LIMIT", e.getMessage()));
    }
    @ExceptionHandler(com.hwanghj.dietmanager.refactoring.backend.food.FoodApiLimitException.class)
    public ResponseEntity<ErrorResponse> handleFoodLimit(RuntimeException e) {
        return ResponseEntity.status(429).header("Retry-After", "60")
            .body(ErrorResponse.of("FOOD_API_LIMIT", e.getMessage()));
    }
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleStatus(org.springframework.web.server.ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(ErrorResponse.of("VALIDATION_ERROR", e.getReason() == null ? "요청을 확인해주세요." : e.getReason()));
    }
    @ExceptionHandler({org.springframework.http.converter.HttpMessageNotReadableException.class,
        org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
        org.springframework.web.bind.MissingServletRequestParameterException.class})
    public ResponseEntity<ErrorResponse> handleMalformedInput(Exception e) {
        return ResponseEntity.badRequest().body(ErrorResponse.of("VALIDATION_ERROR", "입력 형식과 필수 항목을 확인해주세요."));
    }
    @ExceptionHandler(StateConflictException.class)
    public ResponseEntity<ErrorResponse> handleIntakeException(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of("INTAKE_CONFLICT", e.getMessage()));
    }
    @ExceptionHandler({com.hwanghj.dietmanager.refactoring.backend.intake.IntakeNotFoundException.class, com.hwanghj.dietmanager.refactoring.backend.plan.MealPlanNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleIntakeNotFound(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.of("INTAKE_NOT_FOUND", e.getMessage()));
    }
    @ExceptionHandler(com.hwanghj.dietmanager.refactoring.backend.food.FoodApiException.class)
    public ResponseEntity<ErrorResponse> handleFoodApiException(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ErrorResponse.of("FOOD_API_ERROR", e.getMessage()));
    }

    @ExceptionHandler({jakarta.validation.ConstraintViolationException.class,
        org.springframework.web.method.annotation.HandlerMethodValidationException.class})
    public ResponseEntity<ErrorResponse> handleParameterValidation(Exception e) {
        return ResponseEntity.badRequest().body(ErrorResponse.of("VALIDATION_ERROR", "검색어와 페이지 입력을 확인해주세요."));
    }
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

    @ExceptionHandler(BodyMeasurementNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBodyMeasurementNotFoundException(BodyMeasurementNotFoundException e) {
        ErrorResponse response = ErrorResponse.of("BODY_MEASUREMENT_NOT_FOUND", e.getMessage());

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)   // 404 Not Found. 서버는 살아있지만, 요청한 리소스를 찾을 수 없음.
            .body(response);
    }

}
