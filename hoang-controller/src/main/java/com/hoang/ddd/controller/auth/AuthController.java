package com.hoang.ddd.controller.auth;

import com.hoang.ddd.application.auth.dto.LoginCommand;
import com.hoang.ddd.application.auth.dto.LoginUseCase;
import com.hoang.ddd.application.auth.dto.RegisterCommand;
import com.hoang.ddd.application.auth.dto.RegisterUseCase;
import com.hoang.ddd.application.auth.dto.TokenResponse;
import com.hoang.ddd.application.auth.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegisterUseCase registerUseCase;
    private final LoginUseCase loginUseCase;

    // Constructor Injection tường minh
    public AuthController(RegisterUseCase registerUseCase, LoginUseCase loginUseCase) {
        this.registerUseCase = registerUseCase;
        this.loginUseCase = loginUseCase;
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
}
