package com.hwanghj.dietmanager.refactoring.backend.user;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hwanghj.dietmanager.refactoring.backend.exception.DuplicateEmailException;
import com.hwanghj.dietmanager.refactoring.backend.exception.InvalidLoginException;

@Service
public class AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.hwanghj.dietmanager.refactoring.backend.security.AuthSessionService sessions;
    public AuthenticationService(UserRepository userRepository, PasswordEncoder passwordEncoder, com.hwanghj.dietmanager.refactoring.backend.security.AuthSessionService sessions) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessions = sessions;
    }
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

    @Transactional
    public UserLoginDto.Response login(UserLoginDto.Request request) {
        // 요청 DTO 검증.
            // 컨트롤러에서 Valid 어노테이션으로 실행함.
        
        // 사용자 조회, user객체 생성.
        User user = userRepository
                        .findByEmail(request.getEmail())            // email로 사용자 조회.
                        .orElseThrow(InvalidLoginException::new);   // 등록된 사용자가 없는 경우 로그인 실패 예외 발생.
        
        // 비밀번호 검증.
        // passwordEncoder 내부에서 원본 Password를 해싱된 비밀번호와 같은 조건으로 검증.
        if(!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())){
            throw new InvalidLoginException();      // 두 비밀번호가 다르면 로그인 실패 예외 발생.
        }

        // Jwt 토큰 생성.
        var pair = sessions.open(user);
        String accessToken = pair.accessToken();

        // 성공 응답 반환.
        return UserLoginDto.Response
            .builder()
            .userId(user.getId())
            .email(user.getEmail())
            .userName(user.getUserName())
            .role(user.getRole())
            .accessToken(accessToken)
            .refreshToken(pair.refreshToken())
            .build();

    }

    private String hashedPassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
}
