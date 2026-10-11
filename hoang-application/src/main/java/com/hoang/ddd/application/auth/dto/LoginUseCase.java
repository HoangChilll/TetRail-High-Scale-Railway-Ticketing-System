package com.hoang.ddd.application.auth.dto;

import com.hoang.ddd.domain.model.entity.RefreshToken;
import com.hoang.ddd.domain.model.entity.User;
import com.hoang.ddd.domain.repository.RefreshTokenRepository;
import com.hoang.ddd.infrastructure.security.JwtTokenProvider;
import com.hoang.ddd.infrastructure.security.UserDetailsCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoginUseCase {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    @Value("${jwt.refresh-expiration:604800000}")
    private long jwtRefreshExpirationMs;

    @Transactional
    public TokenResponse login(LoginCommand command) {
        // 1. Xác thực thông tin đăng nhập
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                command.getUsernameOrEmail(), command.getPassword());
        Authentication authentication = authenticationManager.authenticate(authenticationToken);

        // 2. Lấy thông tin User đã xác thực thành công
        UserDetailsCustom userDetails = (UserDetailsCustom) authentication.getPrincipal();
        User user = userDetails.getUser();

        // 3. Sinh Access Token (JWT)
        String accessToken = jwtTokenProvider.generateToken(authentication);

        // 4. Sinh Refresh Token ngẫu nhiên (UUID) và lưu vào Database
        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiryDate(Instant.now().plusMillis(jwtRefreshExpirationMs))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshTokenEntity);

        // 5. Trả về cặp Token
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenEntity.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtExpirationMs / 1000)
                .build();
    }
}
