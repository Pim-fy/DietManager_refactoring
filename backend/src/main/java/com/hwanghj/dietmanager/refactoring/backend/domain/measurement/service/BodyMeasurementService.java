package com.hwanghj.dietmanager.refactoring.backend.domain.measurement.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hwanghj.dietmanager.refactoring.backend.domain.user.entity.User;
import com.hwanghj.dietmanager.refactoring.backend.domain.measurement.exception.BodyMeasurementNotFoundException;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.exception.UserNotFoundException;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.repository.UserRepository;

import com.hwanghj.dietmanager.refactoring.backend.domain.measurement.exception.MeasurementConflictException;
import com.hwanghj.dietmanager.refactoring.backend.domain.measurement.dto.BodyMeasurementDto;
import com.hwanghj.dietmanager.refactoring.backend.domain.measurement.entity.BodyMeasurement;
import com.hwanghj.dietmanager.refactoring.backend.domain.measurement.repository.BodyMeasurementRepository;
@Service
public class BodyMeasurementService {
    private final UserRepository userRepository;
    private final BodyMeasurementRepository bodyMeasurementRepository;
    public BodyMeasurementService(UserRepository userRepository, BodyMeasurementRepository bodyMeasurementRepository) {
        this.userRepository = userRepository;
        this.bodyMeasurementRepository = bodyMeasurementRepository;
    }
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

    @Transactional(readOnly = true)
    public BodyMeasurementDto.Response findLatestBodyMeasurement(Long userId){
        BodyMeasurement bodyMeasurement = bodyMeasurementRepository
            .findTopByUserIdAndDeletedFalseOrderByMeasuredDateDescIdDesc(userId)
            .orElseThrow(BodyMeasurementNotFoundException::new);
        
        return toBodyMeasurementResponse(bodyMeasurement);
    }

    @Transactional(readOnly = true)
    public List<BodyMeasurementDto.Response> findBodyMeasurements(Long userId){
        return bodyMeasurementRepository
            .findByUserIdAndDeletedFalseOrderByMeasuredDateDescIdDesc(userId)
            .stream()
            .map(this::toBodyMeasurementResponse)
            .toList();
    }

    @Transactional
    public BodyMeasurementDto.Response updateMeasurement(Long userId, Long id, BodyMeasurementDto.Request request) {
        var record = ownedMeasurement(userId, id);
        if (record.isDeleted()) throw new MeasurementConflictException("삭제된 신체 기록은 먼저 복구해주세요.");
        record.update(request.getMeasuredDate(), request.getHeight(), request.getWeight());
        return toBodyMeasurementResponse(record);
    }

    @Transactional
    public void deleteMeasurement(Long userId, Long id) { ownedMeasurement(userId, id).delete(); }

    @Transactional
    public BodyMeasurementDto.Response restoreMeasurement(Long userId, Long id) {
        var record = ownedMeasurement(userId, id); record.restore(); return toBodyMeasurementResponse(record);
    }

    @Transactional(readOnly = true)
    public List<BodyMeasurementDto.Response> deletedMeasurements(Long userId) {
        return bodyMeasurementRepository.findByUserIdAndDeletedTrueOrderByMeasuredDateDescIdDesc(userId).stream().map(this::toBodyMeasurementResponse).toList();
    }

    private BodyMeasurement ownedMeasurement(Long userId, Long id) {
        return bodyMeasurementRepository.findByIdAndUserId(id, userId).orElseThrow(BodyMeasurementNotFoundException::new);
    }

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
