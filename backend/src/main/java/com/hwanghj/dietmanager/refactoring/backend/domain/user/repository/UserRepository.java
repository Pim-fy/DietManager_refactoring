package com.hwanghj.dietmanager.refactoring.backend.domain.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.hwanghj.dietmanager.refactoring.backend.domain.user.entity.User;

import jakarta.persistence.LockModeType;

/**
 * User 엔터티의 DB 접근 담당 <br>
 * 이메일 기반 사용자 조회, 회원가입 중복 검사 처리
 */
public interface UserRepository extends JpaRepository<User, Long> {

    // 값이 null일 수도 있는 경우를 안전하게 다루기 위해 만들어진 Wrapper 클래스. 최대 1개의 값만 가질 수 있음.
    // 기존 if-else로 사용하던 null체크 코드를 체이닝 형태로 사용 가능하게 함.
    // 사용자가 존재하면 Optional<User>로 감싸서 반환하고, 없으면 빈 Optional을 반환하여 NPE 방지.
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByUserName(String userName);

    // 같은 사용자의 "조회 → 없으면 저장" 요청을 줄 세운다. 처음 저장이 동시에 일어나 중복키 오류(500)가 나지 않게 한다.
    // 자식 행 INSERT가 users 행에 공유 잠금을 걸기 때문에, 트랜잭션에서 다른 저장보다 먼저 호출한다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<User> findWithLockById(Long id);

}
