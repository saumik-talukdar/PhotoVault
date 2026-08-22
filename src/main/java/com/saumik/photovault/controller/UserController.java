package com.saumik.photovault.controller;

import com.saumik.photovault.dto.ChangePasswordRequest;
import com.saumik.photovault.dto.RefreshTokenRequest;
import com.saumik.photovault.entity.User;
import com.saumik.photovault.security.AppUserDetails;
import com.saumik.photovault.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PatchMapping("/me/change-password")
    public ResponseEntity<Void> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest changePasswordRequest
    ) {
        User user = ((AppUserDetails) authentication.getPrincipal()).getUser();
        userService.changePassword(
                user,
                changePasswordRequest.currentPassword(),
                changePasswordRequest.newPassword()
        );
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/me/logout")
    public ResponseEntity<Void> logout(
            @Valid @RequestBody RefreshTokenRequest refreshTokenRequest,
            Authentication authentication
    ){
        User user = ((AppUserDetails) authentication.getPrincipal()).getUser();;
        userService.logout(user.getId(), refreshTokenRequest.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/me/logout-all")
    public ResponseEntity<Void> logoutAll(
            Authentication authentication
    ){
        User user = ((AppUserDetails) authentication.getPrincipal()).getUser();
        userService.logoutAll(user.getId());
        return ResponseEntity.noContent().build();
    }
}
