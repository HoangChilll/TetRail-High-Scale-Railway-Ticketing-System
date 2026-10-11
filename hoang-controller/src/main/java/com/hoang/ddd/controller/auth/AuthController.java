package com.hoang.ddd.controller.auth;

import com.hoang.ddd.application.auth.dto.LoginCommand;
import com.hoang.ddd.application.auth.dto.LoginUseCase;
import com.hoang.ddd.application.auth.dto.LogoutCommand;
import com.hoang.ddd.application.auth.dto.RefreshTokenCommand;
import com.hoang.ddd.application.auth.dto.RefreshTokenUseCase;
import com.hoang.ddd.application.auth.dto.RegisterCommand;
import com.hoang.ddd.application.auth.dto.RegisterUseCase;
import com.hoang.ddd.application.auth.dto.TokenResponse;
import com.hoang.ddd.application.auth.dto.UserResponse;
import com.hoang.ddd.infrastructure.security.UserDetailsCustom;
import com.hoang.ddd.infrastructure.security.annotation.CurrentUser;
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

    private final RegisterUseCase registerUseCase;
    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;

    public AuthController(
            RegisterUseCase registerUseCase,
            LoginUseCase loginUseCase,
            RefreshTokenUseCase refreshTokenUseCase) {
        this.registerUseCase = registerUseCase;
        this.loginUseCase = loginUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterCommand command) {
        UserResponse response = registerUseCase.register(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginCommand command) {
        TokenResponse response = loginUseCase.login(command);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenCommand command) {
        TokenResponse response = refreshTokenUseCase.refreshToken(command);
        return ResponseEntity.ok(response);
    }

    /**
     * API ĐĂNG XUẤT: Thu hồi Refresh Token
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@Valid @RequestBody LogoutCommand command) {
        refreshTokenUseCase.logout(command);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@CurrentUser UserDetailsCustom currentUser) {
        UserResponse response = UserResponse.fromEntity(currentUser.getUser());
        return ResponseEntity.ok(response);
    }
}
