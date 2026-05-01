package com.hwanghj.dietmanager.refactoring.backend.exception;

public class DuplicateEmailException extends RuntimeException{
    public DuplicateEmailException() {
        // GlobalExceptionHandler에서 사용할 메시지 전달.
        // DuplicateEmailException -> RuntimeException -> Exception -> Throwable
        // Throwable에서 예외가 어디서 발생했는지 호출 위치 기록, 전달한 메시지 저장하여 예외 객체 생성 완료.
        // 이후 getMessage()를 호출 시 detailMessage를 꺼내오는 구조.
        super("이미 사용 중인 이메일입니다.");
    }
}
