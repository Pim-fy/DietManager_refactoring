package com.hwanghj.dietmanager.refactoring.backend.exception;

public class UserNotFoundException extends RuntimeException{
    
    public UserNotFoundException() {
        /*
            1. GlobalExceptionHandler에서 사용할 메시지 전달.
            2. RuntimeException -> Exception -> Throwable
            3. Throwable
                3-1. fillInStackTrace()
                - 예외가 생성된 순간의 호출 스택을 예외 객체 안에 저장하는 작업이라고 이해하면 됨.
                - 예외가 어디서 발생했는지 호출 경로를 기록.
                - 서비스 코드에서 throw new ... 으로 예외를 던질 경우, JVM은 예외 객체를 만들면서 현재 실행 위치를 추적함.
                3-2. detailMessage = message
                - 처음에 넘긴 예외 메시지를 저장.
                - 이후 e.getMessage() 호출 시 저장된 메시지가 반환되는 것.
        */
        super("사용자를 찾을 수 없습니다.");
    }
}