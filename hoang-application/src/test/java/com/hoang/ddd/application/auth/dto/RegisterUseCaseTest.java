package com.hoang.ddd.application.auth.dto;

import com.hoang.ddd.domain.model.entity.Role;
import com.hoang.ddd.domain.model.entity.User;
import com.hoang.ddd.domain.model.entity.UserStatus;
import com.hoang.ddd.domain.repository.RoleRepository;
import com.hoang.ddd.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterUseCaseTest {

    // 1. Tạo các Mock Object giả lập phụ thuộc bên ngoài
    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    // 2. Tạo đối tượng THẬT và inject 3 mock ở trên vào
    @InjectMocks
    private RegisterUseCase registerUseCase;

    private RegisterCommand command;

    @BeforeEach
    void setUp() {
        // Dữ liệu mẫu dùng chung cho các ca test
        command = RegisterCommand.builder()
                .username("hoang_user")
                .email("hoang@tetrail.vn")
                .password("rawPassword123")
                .build();
    }

    @Test
    @DisplayName("Đăng ký thành công: Hash password, gán role mặc định và lưu User vào DB")
    void register_Success() {
        // ARRANGE (Chuẩn bị kịch bản giả lập)
        when(userRepository.existsByUsername(command.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(command.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(command.getPassword())).thenReturn("$2a$10$encodedHashPassword");

        Role defaultRole = Role.builder().id(1L).name("ROLE_USER").build();
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(defaultRole));

        User savedUser = User.builder()
                .id(100L)
                .username(command.getUsername())
                .email(command.getEmail())
                .password("$2a$10$encodedHashPassword")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(defaultRole))
                .build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // ACT (Thực thi hàm nghiệp vụ)
        UserResponse response = registerUseCase.register(command);
        // ASSERT (Xác minh kết quả trả về)
        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("hoang_user", response.getUsername());
        assertEquals("hoang@tetrail.vn", response.getEmail());
        assertTrue(response.getRoles().contains("ROLE_USER"));

        // VERIFY: Đảm bảo các method bắt buộc được gọi đúng 1 lần
        verify(passwordEncoder, times(1)).encode("rawPassword123");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Đăng ký thất bại khi Username đã tồn tại: Ném ngoại lệ và TUYỆT ĐỐI không được lưu DB")
    void register_Fails_WhenUsernameAlreadyExists() {
        // ARRANGE: Giả lập username đã có trong hệ thống
        when(userRepository.existsByUsername(command.getUsername())).thenReturn(true);

        // ACT & ASSERT: Xác minh ném ra đúng loại ngoại lệ và thông điệp
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> registerUseCase.register(command));

        assertEquals("Username 'hoang_user' is already taken", exception.getMessage());

        // KIỂM TRA BẢO MẬT & TÍNH TOÀN VẸN:
        // Đảm bảo không bao giờ được gọi lệnh save() hay encode() mật khẩu
        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("Đăng ký thất bại khi Email đã tồn tại: Ném ngoại lệ và không được lưu DB")
    void register_Fails_WhenEmailAlreadyExists() {
        // ARRANGE: Username chưa có nhưng Email đã bị trùng
        when(userRepository.existsByUsername(command.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(command.getEmail())).thenReturn(true);

        // ACT & ASSERT
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> registerUseCase.register(command));

        assertEquals("Email 'hoang@tetrail.vn' is already registered", exception.getMessage());

        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
    }
}
