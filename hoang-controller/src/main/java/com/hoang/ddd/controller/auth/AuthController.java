package com.hoang.ddd.controller.auth;

import com.hoang.ddd.application.auth.dto.LoginCommand;
import com.hoang.ddd.application.auth.dto.LoginUseCase;
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

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegisterUseCase registerUseCase;
    private final LoginUseCase loginUseCase;

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

    /**
     * PARAMETER RESOLVER PATTERN: Trích xuất thông tin người dùng đang thực hiện
     * request.
     * Phòng chống hoàn toàn lỗ hổng IDOR.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@CurrentUser UserDetailsCustom currentUser) {
        // currentUser chứa sẵn Domain Entity User (từ Adapter Pattern ở Bước 4)
        UserResponse response = UserResponse.fromEntity(currentUser.getUser());
        return ResponseEntity.ok(response);
    }
}
