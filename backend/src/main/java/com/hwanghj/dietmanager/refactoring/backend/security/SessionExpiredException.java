package com.hwanghj.dietmanager.refactoring.backend.security;

import com.hwanghj.dietmanager.refactoring.backend.exception.BusinessException;
import com.hwanghj.dietmanager.refactoring.backend.exception.ErrorCode;

/** Refresh Token이 없거나 만료·폐기·재사용된 경우. */
public class SessionExpiredException extends BusinessException {
    public SessionExpiredException() { super(ErrorCode.SESSION_EXPIRED); }
}
