package com.hoang.ddd.application.usecase;

import com.hoang.ddd.application.auth.dto.LogoutCommand;
import com.hoang.ddd.application.auth.dto.RefreshTokenCommand;
import com.hoang.ddd.application.auth.dto.TokenResponse;
import com.hoang.ddd.domain.model.entity.RefreshToken;
import com.hoang.ddd.domain.model.entity.Role;
import com.hoang.ddd.domain.model.entity.User;
import com.hoang.ddd.domain.model.entity.UserStatus;
import com.hoang.ddd.domain.repository.RefreshTokenRepository;
import com.hoang.ddd.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenUseCaseTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private RefreshTokenUseCase refreshTokenUseCase;

    private User sampleUser;
    private static final long JWT_EXPIRATION_MS = 86400000L;       // 24 hours
    private static final long REFRESH_EXPIRATION_MS = 604800000L;  // 7 days

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenUseCase, "jwtExpirationMs", JWT_EXPIRATION_MS);
        ReflectionTestUtils.setField(refreshTokenUseCase, "jwtRefreshExpirationMs", REFRESH_EXPIRATION_MS);

        Role role = Role.builder().id(1L).name("ROLE_USER").build();
        sampleUser = User.builder()
                .id(1L)
                .username("hoang_user")
                .email("hoang@tetrail.vn")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(role))
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Làm mới Token thành công (Token Rotation): Thu hồi token cũ, cấp mới refresh token và access token")
    void refreshToken_Success_RotatesTokens() {
        // ARRANGE
        String oldTokenString = "old-refresh-token-uuid-123";
        RefreshTokenCommand command = new RefreshTokenCommand(oldTokenString);

        RefreshToken oldToken = RefreshToken.builder()
                .id(10L)
                .token(oldTokenString)
                .user(sampleUser)
                .expiryDate(Instant.now().plusSeconds(3600)) // Còn hạn
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByToken(oldTokenString)).thenReturn(Optional.of(oldToken));
        when(jwtTokenProvider.generateToken(any(Authentication.class))).thenReturn("new.access.token.jwt");

        // ACT
        TokenResponse response = refreshTokenUseCase.refreshToken(command);

        // ASSERT
        assertNotNull(response);
        assertEquals("new.access.token.jwt", response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertNotEquals(oldTokenString, response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(JWT_EXPIRATION_MS / 1000, response.getExpiresIn());

        // Token cũ phải bị thu hồi (revoked = true)
        assertTrue(oldToken.isRevoked());

        // Xác minh gọi save 2 lần: 1 lần cho oldToken (thu hồi), 1 lần cho newToken (cấp mới)
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, times(2)).save(captor.capture());

        List<RefreshToken> savedTokens = captor.getAllValues();
        // Lần save thứ nhất: token cũ bị thu hồi
        assertEquals(oldTokenString, savedTokens.get(0).getToken());
        assertTrue(savedTokens.get(0).isRevoked());

        // Lần save thứ hai: token mới active
        RefreshToken newToken = savedTokens.get(1);
        assertEquals(response.getRefreshToken(), newToken.getToken());
        assertFalse(newToken.isRevoked());
        assertEquals(sampleUser, newToken.getUser());
        assertTrue(newToken.getExpiryDate().isAfter(Instant.now()));

        verify(jwtTokenProvider, times(1)).generateToken(any(Authentication.class));
    }

    @Test
    @DisplayName("Làm mới Token thất bại khi Token không tồn tại: Ném BadCredentialsException")
    void refreshToken_Fails_WhenTokenNotFound() {
        // ARRANGE
        RefreshTokenCommand command = new RefreshTokenCommand("non-existent-token");
        when(refreshTokenRepository.findByToken("non-existent-token")).thenReturn(Optional.empty());

        // ACT & ASSERT
        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> refreshTokenUseCase.refreshToken(command)
        );

        assertEquals("Invalid refresh token", exception.getMessage());
        verify(jwtTokenProvider, never()).generateToken(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Làm mới Token thất bại khi Token đã bị thu hồi: Ném BadCredentialsException")
    void refreshToken_Fails_WhenTokenAlreadyRevoked() {
        // ARRANGE
        String revokedTokenString = "revoked-token-uuid";
        RefreshTokenCommand command = new RefreshTokenCommand(revokedTokenString);

        RefreshToken revokedToken = RefreshToken.builder()
                .token(revokedTokenString)
                .user(sampleUser)
                .expiryDate(Instant.now().plusSeconds(3600))
                .revoked(true) // Đã bị thu hồi trước đó
                .build();

        when(refreshTokenRepository.findByToken(revokedTokenString)).thenReturn(Optional.of(revokedToken));

        // ACT & ASSERT
        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> refreshTokenUseCase.refreshToken(command)
        );

        assertEquals("Refresh token has expired or been revoked", exception.getMessage());
        verify(jwtTokenProvider, never()).generateToken(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Làm mới Token thất bại khi Token đã hết hạn: Ném BadCredentialsException")
    void refreshToken_Fails_WhenTokenExpired() {
        // ARRANGE
        String expiredTokenString = "expired-token-uuid";
        RefreshTokenCommand command = new RefreshTokenCommand(expiredTokenString);

        RefreshToken expiredToken = RefreshToken.builder()
                .token(expiredTokenString)
                .user(sampleUser)
                .expiryDate(Instant.now().minusSeconds(60)) // Đã quá hạn 60 giây
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByToken(expiredTokenString)).thenReturn(Optional.of(expiredToken));

        // ACT & ASSERT
        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> refreshTokenUseCase.refreshToken(command)
        );

        assertEquals("Refresh token has expired or been revoked", exception.getMessage());
        verify(jwtTokenProvider, never()).generateToken(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Đăng xuất thành công khi Token tồn tại: Thu hồi Token và xóa sạch SecurityContext")
    void logout_Success_WhenTokenExists() {
        // ARRANGE
        String tokenString = "active-logout-token";
        LogoutCommand command = new LogoutCommand(tokenString);

        RefreshToken token = RefreshToken.builder()
                .token(tokenString)
                .user(sampleUser)
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByToken(tokenString)).thenReturn(Optional.of(token));

        // Giả lập thread đang có Authentication
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", "pass")
        );
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());

        // ACT
        refreshTokenUseCase.logout(command);

        // ASSERT
        assertTrue(token.isRevoked());
        verify(refreshTokenRepository, times(1)).save(token);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("Đăng xuất khi Token không tồn tại: Không lưu gì nhưng vẫn xóa sạch SecurityContext")
    void logout_Success_WhenTokenNotFound() {
        // ARRANGE
        String tokenString = "unknown-token";
        LogoutCommand command = new LogoutCommand(tokenString);

        when(refreshTokenRepository.findByToken(tokenString)).thenReturn(Optional.empty());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", "pass")
        );

        // ACT
        refreshTokenUseCase.logout(command);

        // ASSERT
        verify(refreshTokenRepository, never()).save(any());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}

