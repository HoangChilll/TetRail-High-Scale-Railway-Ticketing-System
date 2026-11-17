package com.hoang.service;

import com.hoang.dto.request.LoginRequest;
import com.hoang.dto.request.LogoutRequest;
import com.hoang.dto.request.RefreshTokenRequest;
import com.hoang.dto.request.RegisterRequest;
import com.hoang.dto.response.TokenResponse;
import com.hoang.dto.response.UserResponse;
import com.hoang.model.RefreshToken;
import com.hoang.model.Role;
import com.hoang.model.User;
import com.hoang.model.UserStatus;
import com.hoang.notification.NotificationChannel;
import com.hoang.notification.NotificationMessage;
import com.hoang.repository.RefreshTokenRepository;
import com.hoang.repository.RoleRepository;
import com.hoang.repository.UserRepository;
import com.hoang.security.JwtTokenProvider;
import com.hoang.security.UserDetailsCustom;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;

    private final JwtTokenProvider jwtTokenProvider;

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    @Value("${jwt.refresh-expiration:604800000}")
    private long jwtRefreshExpirationMs;

    private final NotificationService notificationService;

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final PasswordEncoder passwordEncoder;

    @Transactional
    public TokenResponse login(LoginRequest command) {
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

    @Transactional
    public UserResponse register(RegisterRequest command) {
        // 1. Kiểm tra username đã tồn tại chưa
        if (userRepository.existsByUsername(command.getUsername())) {
            throw new IllegalArgumentException("Username '" + command.getUsername() + "' is already taken");
        }

        // 2. Kiểm tra email đã tồn tại chưa
        if (userRepository.existsByEmail(command.getEmail())) {
            throw new IllegalArgumentException("Email '" + command.getEmail() + "' is already registered");
        }

        // 3. Lấy hoặc tự động tạo role mặc định ROLE_USER nếu chưa có trong DB
        Role defaultRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(
                        Role.builder()
                                .name("ROLE_USER")
                                .description("Default standard user role")
                                .build()));

        // 4. Tạo entity User với mật khẩu đã được hash bằng BCrypt
        User user = User.builder()
                .username(command.getUsername())
                .email(command.getEmail())
                .password(passwordEncoder.encode(command.getPassword()))
                .status(UserStatus.ACTIVE)
                .roles(Set.of(defaultRole))
                .build();

        // 5. Lưu vào Database
        User savedUser = userRepository.save(user);

        // 6. Gửi email chào mừng người dùng mới
        NotificationMessage welcomeEmail = NotificationMessage.builder()
                .recipient(savedUser.getEmail())
                .subject("Chào mừng bạn gia nhập hệ thống bán vé tàu TetRail!")
                .content("Xin chào " + savedUser.getUsername() + ", tài khoản của bạn đã được khởi tạo thành công.")
                .build();
        notificationService.send(NotificationChannel.EMAIL, welcomeEmail);
        // 7. Chuyển đổi và trả về DTO phản hồi
        return UserResponse.fromEntity(savedUser);
    }

    @Transactional
    public TokenResponse refreshToken(RefreshTokenRequest command) {
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
    public void logout(LogoutRequest command) {
        refreshTokenRepository.findByToken(command.getRefreshToken())
                .ifPresent(token -> {
                    token.setRevoked(true); // Vô hiệu hóa Refresh Token trong DB
                    refreshTokenRepository.save(token);
                });

        // Xóa sạch thông tin danh tính trong thread hiện tại
        SecurityContextHolder.clearContext();
    }
}
