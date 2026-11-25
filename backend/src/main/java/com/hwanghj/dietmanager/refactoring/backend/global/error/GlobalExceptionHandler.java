package com.hwanghj.dietmanager.refactoring.backend.global.error;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.validation.ConstraintViolationException;

/**
 * 모든 REST 컨트롤러에서 발생하는 예외를 한 곳에서 공통 처리.
 * 응답 본문은 항상 {code, message[, errors]} 형식이다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String MALFORMED_INPUT = "입력 형식과 필수 항목을 확인해주세요.";

    /** 업무 규칙 위반. 예외가 가진 ErrorCode로 상태와 코드를 정한다. */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
        ErrorCode code = e.getErrorCode();
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(code.getStatus());
        if (e.getRetryAfterSeconds() > 0) {
            builder.header(HttpHeaders.RETRY_AFTER, String.valueOf(e.getRetryAfterSeconds()));
        }
        return builder.body(ErrorResponse.of(code.name(), e.getMessage()));
    }

    /** @Valid 요청 본문 검증 실패. 잘못된 필드를 errors로 알려준다. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBodyValidation(MethodArgumentNotValidException e) {
        List<ErrorResponse.FieldError> errors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> new ErrorResponse.FieldError(error.getField(), error.getDefaultMessage()))
                .toList();
        return validation("입력값 검증에 실패했습니다.", errors);
    }

    /** 쿼리 파라미터·경로 변수 검증 실패(@Min, @Max 등). */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleParameterValidation(HandlerMethodValidationException e) {
        List<ErrorResponse.FieldError> errors = e.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new ErrorResponse.FieldError(
                                result.getMethodParameter().getParameterName(), error.getDefaultMessage())))
                .toList();
        return validation(ErrorCode.VALIDATION_ERROR.getMessage(), errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException e) {
        List<ErrorResponse.FieldError> errors = e.getConstraintViolations().stream()
                .map(violation -> new ErrorResponse.FieldError(lastNode(violation.getPropertyPath().toString()), violation.getMessage()))
                .toList();
        return validation(ErrorCode.VALIDATION_ERROR.getMessage(), errors);
    }

    /** 본문 JSON을 읽을 수 없음(형식 오류, 알 수 없는 enum 값 등). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException e) {
        return validation(MALFORMED_INPUT, List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return validation(MALFORMED_INPUT, List.of(new ErrorResponse.FieldError(e.getName(), "형식이 올바르지 않습니다.")));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException e) {
        return validation(MALFORMED_INPUT, List.of(new ErrorResponse.FieldError(e.getParameterName(), "필수 항목입니다.")));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException e) {
        return error(ErrorCode.NOT_FOUND);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return error(ErrorCode.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
        return error(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
    }

    /** 코드에서 직접 던진 ResponseStatusException. 상태에 맞는 공통 코드로 응답한다. */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException e) {
        HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
        ErrorCode code = switch (status == null ? HttpStatus.INTERNAL_SERVER_ERROR : status) {
        case FORBIDDEN -> ErrorCode.FORBIDDEN;
        case NOT_FOUND -> ErrorCode.NOT_FOUND;
        case UNAUTHORIZED -> ErrorCode.UNAUTHORIZED;
        default -> e.getStatusCode().is4xxClientError() ? ErrorCode.VALIDATION_ERROR : ErrorCode.INTERNAL_ERROR;
        };
        String message = e.getReason() == null ? code.getMessage() : e.getReason();
        return ResponseEntity.status(e.getStatusCode()).body(ErrorResponse.of(code.name(), message));
    }

    /** 예상하지 못한 오류. 내부 내용은 응답에 넣지 않고 서버 로그에만 남긴다. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("처리되지 않은 예외", e);
        return error(ErrorCode.INTERNAL_ERROR);
    }

    private ResponseEntity<ErrorResponse> validation(String message, List<ErrorResponse.FieldError> errors) {
        return ResponseEntity.badRequest().body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR, message, errors));
    }

    private ResponseEntity<ErrorResponse> error(ErrorCode code) {
        return ResponseEntity.status(code.getStatus()).body(ErrorResponse.of(code));
    }

    private String lastNode(String path) {
        int dot = path.lastIndexOf('.');
        return dot < 0 ? path : path.substring(dot + 1);
    }
}
