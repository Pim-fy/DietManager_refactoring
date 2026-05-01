package com.hwanghj.dietmanager.refactoring.backend.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hwanghj.dietmanager.refactoring.backend.dto.UserRegisterDto;
import com.hwanghj.dietmanager.refactoring.backend.entity.User;
import com.hwanghj.dietmanager.refactoring.backend.entity.UserProfile;
import com.hwanghj.dietmanager.refactoring.backend.exception.DuplicateEmailException;
import com.hwanghj.dietmanager.refactoring.backend.repository.UserRepository;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // 회원가입
    @Transactional
    public UserRegisterDto.Response register(UserRegisterDto.Request request) {
        // 기존 사용중인 아이디 여부 확인
        if(userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException();
        }

        // 비밀번호 해싱 호출
        String hashedPassword = hashedPassword(request.getPassword());

        // User 생성
        User user = User.builder()
                        .email(request.getEmail())
                        .passwordHash(hashedPassword)
                        .userName(request.getUserName())
                        .build();
        
        // UserProfile 생성
        UserProfile userProfile = UserProfile.builder()
                                                .gender(request.getGender())
                                                .birthDate(request.getBirthDate())
                                                .goalType(request.getGoalType())
                                                .build();
        
        // User에 UserProfile 연결
        user.assignProfile(userProfile);

        // save 시점의 DB 중복 예외 처리
        try {
            // User저장. 연결된 UserProfile도 자동으로 같이 저장됨.
            userRepository.save(user);
        // DataIntegrityViolationException: DB 무결성 제약조건이 깨졌을 때 Spring이 던지는 예외.
        } catch(DataIntegrityViolationException e) {
                throw new DuplicateEmailException();
        }

        // 응답 반환
        return new UserRegisterDto.Response("회원가입이 완료되었습니다.");
        
    }

    // 비밀번호 해싱
    private String hashedPassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    // 회원가입
    // 로그인
    // 계정 정보 조회/수정
    // 비밀번호 변경
    // 프로필 조회/수정
    // 신체 측정값 저장/조회
}
