package com.hwanghj.dietmanager.refactoring.backend.domain.measurement.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hwanghj.dietmanager.refactoring.backend.domain.measurement.dto.BodyMeasurementDto;
import com.hwanghj.dietmanager.refactoring.backend.domain.measurement.service.BodyMeasurementService;
import com.hwanghj.dietmanager.refactoring.backend.global.security.CustomUserDetails;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class BodyMeasurementController {
    private final BodyMeasurementService bodyMeasurementService;
    public BodyMeasurementController(BodyMeasurementService bodyMeasurementService) {
        this.bodyMeasurementService = bodyMeasurementService;
    }

    @GetMapping("/body-measurements/deleted")
    public List<BodyMeasurementDto.Response> deletedMeasurements(@AuthenticationPrincipal CustomUserDetails user) {
        return bodyMeasurementService.deletedMeasurements(user.getUserId());
    }

    @PostMapping("/body-measurements")
    public ResponseEntity<BodyMeasurementDto.Response> addBodyMeasurement(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody BodyMeasurementDto.Request request){
        
        BodyMeasurementDto.Response response = bodyMeasurementService.addBodyMeasurement(userDetails.getUserId(), request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/body-measurements/latest")
    public ResponseEntity<BodyMeasurementDto.Response> findLatestBodyMeasurement(
            @AuthenticationPrincipal CustomUserDetails userDetails){

        return ResponseEntity.ok(
            bodyMeasurementService.findLatestBodyMeasurement(userDetails.getUserId())
        );
    }

    @GetMapping("/body-measurements")
    public ResponseEntity<List<BodyMeasurementDto.Response>> findBodyMeasurements(
            @AuthenticationPrincipal CustomUserDetails userDetails){
        return ResponseEntity.ok(
            bodyMeasurementService.findBodyMeasurements(userDetails.getUserId())
        );
    }
    @PatchMapping("/body-measurements/{id}")
    public BodyMeasurementDto.Response updateMeasurement(@AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id, @Valid @RequestBody BodyMeasurementDto.Request request) {
        return bodyMeasurementService.updateMeasurement(user.getUserId(), id, request);
    }
    @DeleteMapping("/body-measurements/{id}")
    public ResponseEntity<Void> deleteMeasurement(@AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id) {
        bodyMeasurementService.deleteMeasurement(user.getUserId(), id);
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/body-measurements/{id}/restore")
    public BodyMeasurementDto.Response restoreMeasurement(@AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id) {
        return bodyMeasurementService.restoreMeasurement(user.getUserId(), id);
    }
}
