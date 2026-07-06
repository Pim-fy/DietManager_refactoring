package com.hwanghj.dietmanager.refactoring.backend.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hwanghj.dietmanager.refactoring.backend.dto.BodyMeasurementDto;
import com.hwanghj.dietmanager.refactoring.backend.dto.PasswordChangeDto;
import com.hwanghj.dietmanager.refactoring.backend.dto.UserAccountFindDto;
import com.hwanghj.dietmanager.refactoring.backend.dto.UserAccountModifyDto;
import com.hwanghj.dietmanager.refactoring.backend.dto.UserLoginDto;
import com.hwanghj.dietmanager.refactoring.backend.dto.UserProfileDto;
import com.hwanghj.dietmanager.refactoring.backend.dto.UserRegisterDto;
import com.hwanghj.dietmanager.refactoring.backend.entity.BodyMeasurement;
import com.hwanghj.dietmanager.refactoring.backend.entity.User;
import com.hwanghj.dietmanager.refactoring.backend.entity.UserProfile;
import com.hwanghj.dietmanager.refactoring.backend.exception.BodyMeasurementNotFoundException;
import com.hwanghj.dietmanager.refactoring.backend.exception.DuplicateEmailException;
import com.hwanghj.dietmanager.refactoring.backend.exception.InvalidLoginException;
import com.hwanghj.dietmanager.refactoring.backend.exception.InvalidPasswordException;
import com.hwanghj.dietmanager.refactoring.backend.exception.UserNotFoundException;
import com.hwanghj.dietmanager.refactoring.backend.exception.UserProfileNotFoundException;
import com.hwanghj.dietmanager.refactoring.backend.repository.BodyMeasurementRepository;
import com.hwanghj.dietmanager.refactoring.backend.repository.UserProfileRepository;
import com.hwanghj.dietmanager.refactoring.backend.repository.UserRepository;
import com.hwanghj.dietmanager.refactoring.backend.security.JwtTokenProvider;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserProfileRepository userProfileRepository;
    private final BodyMeasurementRepository bodyMeasurementRepository;

    public UserService(
            UserRepository userRepository, 
            PasswordEncoder passwordEncoder, 
            JwtTokenProvider jwtTokenProvider,
            UserProfileRepository userProfileRepository,
            BodyMeasurementRepository bodyMeasurementRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userProfileRepository = userProfileRepository;
        this.bodyMeasurementRepository = bodyMeasurementRepository;
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


    // 로그인
    @Transactional(readOnly = true)
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
        String accessToken = jwtTokenProvider.createAccessToken(user);

        // 성공 응답 반환.
        return UserLoginDto.Response
            .builder()
            .userId(user.getId())
            .email(user.getEmail())
            .userName(user.getUserName())
            .role(user.getRole())
            .accessToken(accessToken)
            .build();

    }

    // 계정 정보 조회.
    @Transactional(readOnly = true)
    public UserAccountFindDto.Response findAccount(Long userId) {
        // user 객체 생성.
        User user = userRepository
            .findById(userId)
            .orElseThrow(UserNotFoundException::new);
            // .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return toAccountResponse(user);
    }

    // 계정 정보 수정
    @Transactional
    public UserAccountModifyDto.Response modifyAccount(
            Long userId, UserAccountModifyDto.Request request) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
            // .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if(!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException();
        }

        user.updateAccount(request.getEmail(), request.getUserName());

        return toAccountModifyResponse(user);

    }

    // 비밀번호 변경
    @Transactional
    public PasswordChangeDto.Response changePassword(
            Long userId, PasswordChangeDto.Request request) {
        User user = userRepository.findById(userId)
            .orElseThrow(UserNotFoundException::new);
        if(!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new InvalidPasswordException();
        }
        String hashedPassword = hashedPassword(request.getNewPassword());
        user.updatePassword(hashedPassword);
        return new PasswordChangeDto.Response("비밀번호가 변경되었습니다.");
    }

    // 프로필 조회
    @Transactional(readOnly = true)
    public UserProfileDto.Response findProfile(Long userId){
        UserProfile userProfile = userProfileRepository
            .findByUserId(userId)
            .orElseThrow(UserProfileNotFoundException::new);
            // == .orElseThrow(() -> new UserProfileNotFoundException());
        
        return toProfileResponse(userProfile);
    }

    // 프로필 수정
    @Transactional
    public UserProfileDto.Response modifyProfile(
            Long userId,
            UserProfileDto.Request request){
        UserProfile userProfile = userProfileRepository
            .findByUserId(userId)
            .orElseThrow(UserProfileNotFoundException::new);

        userProfile.updateProfile(
            request.getGender(), 
            request.getBirthDate(), 
            request.getGoalType());
        
        return toProfileResponse(userProfile);
    }

    // 신체 측정값 저장.
    @Transactional
    public BodyMeasurementDto.Response addBodyMeasurement(
            Long userId,
            BodyMeasurementDto.Request request){

        User user = userRepository
            .findById(userId)
            .orElseThrow(UserNotFoundException::new);

        BodyMeasurement bodyMeasurement = BodyMeasurement
            .builder()
            .user(user)
            .measuredDate(request.getMeasuredDate())
            .height(request.getHeight())
            .weight(request.getWeight())
            .build();

        BodyMeasurement savedBodyMeasurement = bodyMeasurementRepository.save(bodyMeasurement);
        
        return toBodyMeasurementResponse(savedBodyMeasurement);
    }

    // 가장 최근 신체 측정값 조회.
    @Transactional(readOnly = true)
    public BodyMeasurementDto.Response findLatestBodyMeasurement(Long userId){
        BodyMeasurement bodyMeasurement = bodyMeasurementRepository
            .findTopByUserIdOrderByMeasuredDateDesc(userId)
            .orElseThrow(BodyMeasurementNotFoundException::new);
        
        return toBodyMeasurementResponse(bodyMeasurement);
    }

    // 전체 신체 측정 이력 조회
    @Transactional(readOnly = true)
    public List<BodyMeasurementDto.Response> findBodyMeasurements(Long userId){
        return bodyMeasurementRepository
            .findByUserIdOrderByMeasuredDateDesc(userId)
            .stream()
            .map(this::toBodyMeasurementResponse)
            .toList();
    }

    // 헬퍼 메서드.

    // 로그인 응답 헬퍼 메서드.
    private UserAccountFindDto.Response toAccountResponse(User user) {
        return UserAccountFindDto.Response
            .builder()
            .userId(user.getId())   
            .email(user.getEmail())
            .userName(user.getUserName())
            .role(user.getRole())
            .build();
    }

    // 계정 정보 수정 헬퍼 메서드.
    private UserAccountModifyDto.Response toAccountModifyResponse(User user) {
        return UserAccountModifyDto.Response
            .builder()
            .userId(user.getId())
            .email(user.getEmail())
            .userName(user.getUserName())
            .role(user.getRole())
            .build();
    }

    // 비밀번호 해싱
    private String hashedPassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    // 프로필 응답 헬퍼 메서드.
    private UserProfileDto.Response toProfileResponse(UserProfile userProfile){
        return UserProfileDto.Response
            .builder()
            .userId(userProfile.getUser().getId())
            .gender(userProfile.getGender())
            .birthDate(userProfile.getBirthDate())
            .goalType(userProfile.getGoalType())
            .build();
    }

    // 신체 측정값 응답 헬퍼 메서드
    private BodyMeasurementDto.Response toBodyMeasurementResponse(BodyMeasurement bodyMeasurement){
        return BodyMeasurementDto.Response
            .builder()
            .measurementId(bodyMeasurement.getId())
            .measuredDate(bodyMeasurement.getMeasuredDate())
            .height(bodyMeasurement.getHeight())
            .weight(bodyMeasurement.getWeight())
            .build();
    }
}
