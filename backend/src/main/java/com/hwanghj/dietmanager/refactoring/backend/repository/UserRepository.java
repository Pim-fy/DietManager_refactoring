package com.hwanghj.dietmanager.refactoring.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.hwanghj.dietmanager.refactoring.backend.entity.User;

/**
 * User 엔터티의 DB 접근 담당 <br>
 * 이메일 기반 사용자 조회, 회원가입 중복 검사 처리
 */
public interface UserRepository extends JpaRepository<User, Long>{
    
    // 값이 null일 수도 있는 경우를 안전하게 다루기 위해 만들어진 Wrapper 클래스. 최대 1개의 값만 가질 수 있음. 
    // 기존 if-else로 사용하던 null체크 코드를 체이닝 형태로 사용 가능하게 함. 
    // 사용자가 존재하면 Optional<User>로 감싸서 반환하고, 없으면 빈 Optional을 반환하여 NPE 방지.
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByUserName(String userName);

}
