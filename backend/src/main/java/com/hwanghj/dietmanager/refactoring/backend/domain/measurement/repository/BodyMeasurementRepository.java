package com.hwanghj.dietmanager.refactoring.backend.domain.measurement.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hwanghj.dietmanager.refactoring.backend.domain.measurement.entity.BodyMeasurement;

/**
 * BodyMeasurement 엔터티의 DB접근 담당 <br>
 * 신체 측정 이력 저장, 최신/전체 측정 기록 조회
 */
public interface BodyMeasurementRepository extends JpaRepository<BodyMeasurement, Long> {

    // User.java의 id로 BodyMeasurement를 최신순으로 찾아 가장 최근 항목을 Optional 형태로 BodyMeasurement객체를 반환함.
    Optional<BodyMeasurement> findTopByUserIdAndDeletedFalseOrderByMeasuredDateDescIdDesc(Long userId);

    // User.java의 id로 모든 BodyMeasurement를 최신순으로 찾아 List 형태로 BodyMeasurement객체들을 반환함.
    List<BodyMeasurement> findByUserIdAndDeletedFalseOrderByMeasuredDateDescIdDesc(Long userId);

    Optional<BodyMeasurement> findByIdAndUserId(Long id, Long userId);

    List<BodyMeasurement> findByUserIdAndDeletedTrueOrderByMeasuredDateDescIdDesc(Long userId);

    List<BodyMeasurement> findByUserIdAndDeletedFalseAndMeasuredDateBetweenOrderByMeasuredDateAscIdAsc(Long userId, LocalDate start, LocalDate end);

    // 날짜 이전의 삭제되지 않은 신체 기록(측정일·id 오름차순). 날짜별 권장량 계산에 쓴다.
    List<BodyMeasurement> findByUserIdAndDeletedFalseAndMeasuredDateLessThanEqualOrderByMeasuredDateAscIdAsc(Long userId, LocalDate date);
}
