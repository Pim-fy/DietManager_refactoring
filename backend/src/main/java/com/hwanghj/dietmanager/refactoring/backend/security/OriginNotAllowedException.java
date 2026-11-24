package com.hwanghj.dietmanager.refactoring.backend.security;

import com.hwanghj.dietmanager.refactoring.backend.exception.BusinessException;
import com.hwanghj.dietmanager.refactoring.backend.exception.ErrorCode;

/** 쿠키를 쓰는 갱신·로그아웃 요청의 출처가 허용 범위 밖인 경우. */
public class OriginNotAllowedException extends BusinessException {
    public OriginNotAllowedException() { super(ErrorCode.FORBIDDEN); }
}
