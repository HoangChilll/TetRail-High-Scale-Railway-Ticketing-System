package com.hoang.ddd.domain.model.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class RefreshTokenTest {

    @Test
    @DisplayName("isExpired() trả về false khi ngày hết hạn ở tương lai")
    void isExpired_ReturnsFalse_WhenExpiryDateIsInFuture() {
        RefreshToken token = RefreshToken.builder()
                .token("valid-uuid-token")
                .expiryDate(Instant.now().plus(7, ChronoUnit.DAYS))
                .revoked(false)
                .build();

        assertFalse(token.isExpired());
    }

    @Test
    @DisplayName("isExpired() trả về true khi ngày hết hạn ở quá khứ")
    void isExpired_ReturnsTrue_WhenExpiryDateIsInPast() {
        RefreshToken token = RefreshToken.builder()
                .token("expired-uuid-token")
                .expiryDate(Instant.now().minus(1, ChronoUnit.MINUTES))
                .revoked(false)
                .build();

        assertTrue(token.isExpired());
    }

    @Test
    @DisplayName("Giá trị mặc định của trường revoked là false")
    void defaultRevokedStatus_IsFalse() {
        RefreshToken token = RefreshToken.builder()
                .token("token-test")
                .expiryDate(Instant.now().plusSeconds(3600))
                .build();

        assertFalse(token.isRevoked());
    }
}

