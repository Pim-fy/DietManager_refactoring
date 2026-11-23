package com.hwanghj.dietmanager.refactoring.backend.measurement;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hwanghj.dietmanager.refactoring.backend.security.CustomUserDetails;

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
    @org.springframework.web.bind.annotation.PatchMapping("/body-measurements/{id}")
    public BodyMeasurementDto.Response updateMeasurement(@AuthenticationPrincipal CustomUserDetails user,
            @org.springframework.web.bind.annotation.PathVariable Long id, @Valid @RequestBody BodyMeasurementDto.Request request) {
        return bodyMeasurementService.updateMeasurement(user.getUserId(), id, request);
    }
    @org.springframework.web.bind.annotation.DeleteMapping("/body-measurements/{id}")
    public ResponseEntity<Void> deleteMeasurement(@AuthenticationPrincipal CustomUserDetails user,
            @org.springframework.web.bind.annotation.PathVariable Long id) {
        bodyMeasurementService.deleteMeasurement(user.getUserId(), id);
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/body-measurements/{id}/restore")
    public BodyMeasurementDto.Response restoreMeasurement(@AuthenticationPrincipal CustomUserDetails user,
            @org.springframework.web.bind.annotation.PathVariable Long id) {
        return bodyMeasurementService.restoreMeasurement(user.getUserId(), id);
    }
}
