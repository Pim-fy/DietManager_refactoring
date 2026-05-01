package com.hwanghj.dietmanager.refactoring.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.hwanghj.dietmanager.refactoring.backend.entity.UserProfile;

/**
 * UserProfile 엔터티의 DB 접근 담당 <br>
 * 사용자 id 기준 프로필 정보 조회
 */
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    
    // User.java의 id로 UserProfile을 찾아 Optional 형태로 UserProfile객체를 반환함.
    Optional<UserProfile> findByUserId(Long userId);
}
