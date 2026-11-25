package com.hwanghj.dietmanager.refactoring.backend.domain.user.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.hwanghj.dietmanager.refactoring.backend.domain.user.repository.UserRepository;

/**
 * DB 무결성 예외가 났을 때 그 원인이 이메일 중복인지 확인한다.
 * 예외가 난 트랜잭션은 다시 쓸 수 없으므로 새 트랜잭션에서 조회한다.
 */
@Component
public class EmailUsageChecker {
    private final UserRepository userRepository;

    public EmailUsageChecker(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public boolean isTaken(String email) {
        return userRepository.existsByEmail(email);
    }
}
