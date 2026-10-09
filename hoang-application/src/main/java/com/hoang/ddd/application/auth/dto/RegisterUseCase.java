package com.hoang.ddd.application.auth.dto;

import com.hoang.ddd.domain.model.entity.Role;
import com.hoang.ddd.domain.model.entity.User;
import com.hoang.ddd.domain.model.entity.UserStatus;
import com.hoang.ddd.domain.repository.RoleRepository;
import com.hoang.ddd.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class RegisterUseCase {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse register(RegisterCommand command) {
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

        // 6. Chuyển đổi và trả về DTO phản hồi
        return UserResponse.fromEntity(savedUser);
    }
}
