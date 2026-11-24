package com.hwanghj.dietmanager.refactoring.backend.profile;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hwanghj.dietmanager.refactoring.backend.global.security.CustomUserDetails;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class ProfileController {
    private final ProfileService profileService;
    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileDto.Response> findProfile(@AuthenticationPrincipal CustomUserDetails userDetails){
        return ResponseEntity.ok(
            profileService.findProfile(userDetails.getUserId()));
    }

    @PatchMapping("/profile")
    public ResponseEntity<UserProfileDto.Response> modifyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UserProfileDto.Request request){
        return ResponseEntity.ok(
            profileService.modifyProfile(userDetails.getUserId(), request));
    }
}
