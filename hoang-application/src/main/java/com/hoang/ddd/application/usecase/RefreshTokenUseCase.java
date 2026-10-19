package com.hoang.ddd.application.usecase;

import com.hoang.ddd.application.auth.dto.LogoutCommand;
import com.hoang.ddd.application.auth.dto.RefreshTokenCommand;
import com.hoang.ddd.application.auth.dto.TokenResponse;
import com.hoang.ddd.domain.model.entity.RefreshToken;
import com.hoang.ddd.domain.model.entity.User;
import com.hoang.ddd.domain.repository.RefreshTokenRepository;
import com.hoang.ddd.infrastructure.security.JwtTokenProvider;
import com.hoang.ddd.infrastructure.security.UserDetailsCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenUseCase {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    @Value("${jwt.refresh-expiration:604800000}")
    private long jwtRefreshExpirationMs;

    @Transactional
    public TokenResponse refreshToken(RefreshTokenCommand command) {
        RefreshToken oldRefreshToken = refreshTokenRepository.findByToken(command.getRefreshToken())
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if (oldRefreshToken.isRevoked() || oldRefreshToken.isExpired()) {
            throw new BadCredentialsException("Refresh token has expired or been revoked");
        }

        // TOKEN ROTATION: Thu hồi token cũ
        oldRefreshToken.setRevoked(true);
        refreshTokenRepository.save(oldRefreshToken);

        User user = oldRefreshToken.getUser();

        // Cấp token mới
        RefreshToken newRefreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiryDate(Instant.now().plusMillis(jwtRefreshExpirationMs))
                .revoked(false)
                .build();
        refreshTokenRepository.save(newRefreshToken);

        UserDetailsCustom userDetails = new UserDetailsCustom(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        String newAccessToken = jwtTokenProvider.generateToken(authentication);

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtExpirationMs / 1000)
                .build();
    }

    /**
     * THU HỒI TOKEN KHI ĐĂNG XUẤT
     */
    @Transactional
    public void logout(LogoutCommand command) {
        refreshTokenRepository.findByToken(command.getRefreshToken())
                .ifPresent(token -> {
                    token.setRevoked(true); // Vô hiệu hóa Refresh Token trong DB
                    refreshTokenRepository.save(token);
                });

        // Xóa sạch thông tin danh tính trong thread hiện tại
        SecurityContextHolder.clearContext();
    }
}
