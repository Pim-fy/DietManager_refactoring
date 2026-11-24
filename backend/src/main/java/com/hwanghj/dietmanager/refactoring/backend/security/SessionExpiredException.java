package com.hwanghj.dietmanager.refactoring.backend.security;

import com.hwanghj.dietmanager.refactoring.backend.global.error.BusinessException;
import com.hwanghj.dietmanager.refactoring.backend.global.error.ErrorCode;

/** Refresh Token이 없거나 만료·폐기·재사용된 경우. */
public class SessionExpiredException extends BusinessException {
    public SessionExpiredException() { super(ErrorCode.SESSION_EXPIRED); }
}
