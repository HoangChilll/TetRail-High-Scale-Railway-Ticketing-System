package com.hoang.controller;

import com.hoang.dto.request.LoginRequest;
import com.hoang.service.AuthService;
import com.hoang.dto.request.LogoutRequest;
import com.hoang.dto.request.RefreshTokenRequest;
import com.hoang.dto.request.RegisterRequest;
import com.hoang.dto.response.TokenResponse;
import com.hoang.dto.response.UserResponse;
import com.hoang.security.UserDetailsCustom;
import com.hoang.security.annotation.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest command) {
        UserResponse response = authService.register(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest command) {
        TokenResponse response = authService.login(command);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest command) {
        TokenResponse response = authService.refreshToken(command);
        return ResponseEntity.ok(response);
    }

    /**
     * API ĐĂNG XUẤT: Thu hồi Refresh Token
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@Valid @RequestBody LogoutRequest command) {
        authService.logout(command);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@CurrentUser UserDetailsCustom currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserResponse response = UserResponse.fromEntity(currentUser.getUser());
        return ResponseEntity.ok(response);
    }
}
