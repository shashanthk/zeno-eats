package com.zenoeats.userservice.controller;

import com.zenoeats.shared.dto.ApiResponse;
import com.zenoeats.userservice.dto.UserProfileResponse;
import com.zenoeats.userservice.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser(
        @AuthenticationPrincipal User currentUser
    ) {
        UserProfileResponse profile = UserProfileResponse.builder()
            .id(currentUser.getId())
            .email(currentUser.getEmail())
            .firstName(currentUser.getFirstName())
            .lastName(currentUser.getLastName())
            .role(currentUser.getRole().name())
            .build();
        return ResponseEntity.ok(ApiResponse.success(profile));
    }
}
