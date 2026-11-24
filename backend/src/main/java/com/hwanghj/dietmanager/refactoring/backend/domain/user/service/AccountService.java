package com.hwanghj.dietmanager.refactoring.backend.domain.user.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hwanghj.dietmanager.refactoring.backend.domain.user.dto.PasswordChangeDto;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.dto.UserAccountFindDto;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.dto.UserAccountModifyDto;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.entity.User;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.exception.DuplicateEmailException;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.exception.InvalidPasswordException;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.exception.UserNotFoundException;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.repository.UserRepository;

@Service
public class AccountService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserAccountFindDto.Response findAccount(Long userId) {
        // user 객체 생성.
        User user = userRepository
                .findById(userId)
                .orElseThrow(UserNotFoundException::new);
            // .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return toAccountResponse(user);
    }

    @Transactional
    public UserAccountModifyDto.Response modifyAccount(
            Long userId, UserAccountModifyDto.Request request) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
            // .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException();
        }

        user.updateAccount(request.getEmail(), request.getUserName());

        // 동시 이메일 변경은 사전 중복 조회만으로 막을 수 없으므로 DB 제약까지 확인한다.
        try {
            userRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateEmailException();
        }

        return toAccountModifyResponse(user);

    }

    @Transactional
    public PasswordChangeDto.Response changePassword(
            Long userId, PasswordChangeDto.Request request) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new InvalidPasswordException();
        }
        String hashedPassword = hashedPassword(request.getNewPassword());
        user.updatePassword(hashedPassword);
        user.revokeTokens();
        return new PasswordChangeDto.Response("비밀번호가 변경되었습니다.");
    }

    private UserAccountFindDto.Response toAccountResponse(User user) {
        return UserAccountFindDto.Response
                .builder()
                .userId(user.getId())
                .email(user.getEmail())
                .userName(user.getUserName())
                .role(user.getRole())
                .build();
    }

    private UserAccountModifyDto.Response toAccountModifyResponse(User user) {
        return UserAccountModifyDto.Response
                .builder()
                .userId(user.getId())
                .email(user.getEmail())
                .userName(user.getUserName())
                .role(user.getRole())
                .build();
    }

    private String hashedPassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
}
