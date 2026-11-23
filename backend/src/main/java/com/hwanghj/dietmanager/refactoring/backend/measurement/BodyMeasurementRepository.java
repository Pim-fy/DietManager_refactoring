package com.hwanghj.dietmanager.refactoring.backend.measurement;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * BodyMeasurement 엔터티의 DB접근 담당 <br>
 * 신체 측정 이력 저장, 최신/전체 측정 기록 조회
 */
public interface BodyMeasurementRepository extends JpaRepository<BodyMeasurement, Long>{
    
    // User.java의 id로 BodyMeasurement를 최신순으로 찾아 가장 최근 항목을 Optional 형태로 BodyMeasurement객체를 반환함.
    Optional<BodyMeasurement> findTopByUserIdAndDeletedFalseOrderByMeasuredDateDescIdDesc(Long userId);

    // User.java의 id로 모든 BodyMeasurement를 최신순으로 찾아 List 형태로 BodyMeasurement객체들을 반환함.
    List<BodyMeasurement> findByUserIdAndDeletedFalseOrderByMeasuredDateDescIdDesc(Long userId);
    Optional<BodyMeasurement> findByIdAndUserId(Long id, Long userId);
    List<BodyMeasurement> findByUserIdAndDeletedTrueOrderByMeasuredDateDescIdDesc(Long userId);
    List<BodyMeasurement> findByUserIdAndDeletedFalseAndMeasuredDateBetweenOrderByMeasuredDateAscIdAsc(Long userId, java.time.LocalDate start, java.time.LocalDate end);
    Optional<BodyMeasurement> findTopByUserIdAndDeletedFalseAndMeasuredDateLessThanEqualOrderByMeasuredDateDescIdDesc(Long userId, java.time.LocalDate date);
}
