package com.hoang.ddd.application.usecase;

import com.hoang.ddd.application.auth.dto.LoginCommand;
import com.hoang.ddd.application.auth.dto.TokenResponse;
import com.hoang.ddd.domain.model.entity.RefreshToken;
import com.hoang.ddd.domain.model.entity.Role;
import com.hoang.ddd.domain.model.entity.User;
import com.hoang.ddd.domain.model.entity.UserStatus;
import com.hoang.ddd.domain.repository.RefreshTokenRepository;
import com.hoang.ddd.infrastructure.security.JwtTokenProvider;
import com.hoang.ddd.infrastructure.security.UserDetailsCustom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginUseCaseTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private LoginUseCase loginUseCase;

    private User sampleUser;
    private UserDetailsCustom userDetails;
    private Authentication authentication;

    private static final long JWT_EXPIRATION_MS = 86400000L;       // 24 hours
    private static final long REFRESH_EXPIRATION_MS = 604800000L;  // 7 days

    @BeforeEach
    void setUp() {
        // Cấu hình giá trị thuộc tính @Value thông qua ReflectionTestUtils
        ReflectionTestUtils.setField(loginUseCase, "jwtExpirationMs", JWT_EXPIRATION_MS);
        ReflectionTestUtils.setField(loginUseCase, "jwtRefreshExpirationMs", REFRESH_EXPIRATION_MS);

        Role role = Role.builder().id(1L).name("ROLE_USER").build();
        sampleUser = User.builder()
                .id(100L)
                .username("hoang_user")
                .email("hoang@tetrail.vn")
                .password("$2a$10$encodedPassword")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(role))
                .build();

        userDetails = new UserDetailsCustom(sampleUser);
        authentication = mock(Authentication.class);
    }

    @Test
    @DisplayName("Đăng nhập thành công bằng username: Xác thực, sinh Access Token, tạo Refresh Token lưu DB")
    void login_Success_WithUsername() {
        // ARRANGE
        LoginCommand command = LoginCommand.builder()
                .usernameOrEmail("hoang_user")
                .password("rawPassword123")
                .build();

        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtTokenProvider.generateToken(authentication)).thenReturn("mocked.access.jwt.token");

        // ACT
        TokenResponse response = loginUseCase.login(command);

        // ASSERT
        assertNotNull(response);
        assertEquals("mocked.access.jwt.token", response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(JWT_EXPIRATION_MS / 1000, response.getExpiresIn());

        // Kiểm tra đối tượng RefreshToken được lưu vào Database
        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, times(1)).save(tokenCaptor.capture());

        RefreshToken savedToken = tokenCaptor.getValue();
        assertNotNull(savedToken.getToken());
        assertEquals(sampleUser, savedToken.getUser());
        assertFalse(savedToken.isRevoked());
        assertTrue(savedToken.getExpiryDate().isAfter(Instant.now()));

        // VERIFY: Đảm bảo luồng gọi đúng thứ tự
        verify(authenticationManager, times(1)).authenticate(
                argThat(token -> "hoang_user".equals(token.getPrincipal()) && "rawPassword123".equals(token.getCredentials()))
        );
        verify(jwtTokenProvider, times(1)).generateToken(authentication);
    }

    @Test
    @DisplayName("Đăng nhập thành công bằng email: Xác thực đúng thông tin email")
    void login_Success_WithEmail() {
        // ARRANGE
        LoginCommand command = LoginCommand.builder()
                .usernameOrEmail("hoang@tetrail.vn")
                .password("rawPassword123")
                .build();

        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtTokenProvider.generateToken(authentication)).thenReturn("mocked.jwt.for.email");

        // ACT
        TokenResponse response = loginUseCase.login(command);

        // ASSERT
        assertNotNull(response);
        assertEquals("mocked.jwt.for.email", response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Đăng nhập thất bại khi sai mật khẩu: Ném BadCredentialsException và TUYỆT ĐỐI không sinh token")
    void login_Fails_WhenBadCredentials() {
        // ARRANGE
        LoginCommand command = LoginCommand.builder()
                .usernameOrEmail("hoang_user")
                .password("wrongPassword")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid username or password"));

        // ACT & ASSERT
        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> loginUseCase.login(command)
        );

        assertEquals("Invalid username or password", exception.getMessage());

        // Đảm bảo không bao giờ sinh token hoặc lưu RefreshToken rác vào DB
        verify(jwtTokenProvider, never()).generateToken(any());
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Đăng nhập thất bại khi tài khoản bị khóa: Ném LockedException")
    void login_Fails_WhenAccountLocked() {
        // ARRANGE
        LoginCommand command = LoginCommand.builder()
                .usernameOrEmail("locked_user")
                .password("password123")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new LockedException("User account is locked"));

        // ACT & ASSERT
        assertThrows(LockedException.class, () -> loginUseCase.login(command));

        verify(jwtTokenProvider, never()).generateToken(any());
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Đăng nhập thất bại khi tài khoản bị vô hiệu hóa: Ném DisabledException")
    void login_Fails_WhenAccountDisabled() {
        // ARRANGE
        LoginCommand command = LoginCommand.builder()
                .usernameOrEmail("disabled_user")
                .password("password123")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new DisabledException("User is disabled"));

        // ACT & ASSERT
        assertThrows(DisabledException.class, () -> loginUseCase.login(command));

        verify(jwtTokenProvider, never()).generateToken(any());
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }
}
