package com.hoang.ddd.application.auth.dto;

import com.hoang.ddd.infrastructure.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginUseCase {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    public TokenResponse login(LoginCommand command) {
        // 1. COMMAND PATTERN: Đóng gói thông tin đăng nhập thành Token xác thực chưa
        // kiểm chứng
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                command.getUsernameOrEmail(), command.getPassword());

        // 2. FACADE PATTERN: Uỷ quyền cho AuthenticationManager phối hợp kiểm tra User
        // & Password Hash
        Authentication authentication = authenticationManager.authenticate(authenticationToken);

        // 3. Khi xác thực thành công, sinh JWT Access Token mang thông tin người dùng
        String accessToken = jwtTokenProvider.generateToken(authentication);

        // 4. Đóng gói kết quả trả về cho Client
        return TokenResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(jwtExpirationMs / 1000) // Đổi từ milliseconds sang seconds
                .build();
    }
}
