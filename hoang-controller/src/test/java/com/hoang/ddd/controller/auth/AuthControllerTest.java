package com.hoang.ddd.controller.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoang.ddd.application.auth.dto.*;
import com.hoang.ddd.application.usecase.LoginUseCase;
import com.hoang.ddd.application.usecase.RefreshTokenUseCase;
import com.hoang.ddd.application.usecase.RegisterUseCase;
import com.hoang.ddd.controller.advice.GlobalExceptionHandler;
import com.hoang.ddd.domain.model.entity.Role;
import com.hoang.ddd.domain.model.entity.User;
import com.hoang.ddd.domain.model.entity.UserStatus;
import com.hoang.ddd.infrastructure.security.UserDetailsCustom;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // Tắt bộ lọc Security để tập trung test MVC mapping và validation
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RegisterUseCase registerUseCase;

    @MockBean
    private LoginUseCase loginUseCase;

    @MockBean
    private RefreshTokenUseCase refreshTokenUseCase;

    @Test
    @DisplayName("POST /api/auth/register: Đăng ký thành công trả về 201 Created")
    void register_Success() throws Exception {
        RegisterCommand command = RegisterCommand.builder()
                .username("hoang_dev")
                .email("hoang@tetrail.vn")
                .password("password123")
                .build();

        UserResponse mockResponse = UserResponse.builder()
                .id(1L)
                .username("hoang_dev")
                .email("hoang@tetrail.vn")
                .roles(Set.of("ROLE_USER"))
                .build();

        when(registerUseCase.register(any(RegisterCommand.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.username").value("hoang_dev"))
                .andExpect(jsonPath("$.email").value("hoang@tetrail.vn"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));

        verify(registerUseCase, times(1)).register(any(RegisterCommand.class));
    }

    @Test
    @DisplayName("POST /api/auth/register: Thất bại khi dữ liệu không hợp lệ trả về 400 Bad Request")
    void register_Fails_WhenValidationErrors() throws Exception {
        // Dữ liệu lỗi: username quá ngắn, email sai định dạng, password trống
        RegisterCommand invalidCommand = RegisterCommand.builder()
                .username("ab") // < 3 ký tự
                .email("invalid-email-format")
                .password("")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidCommand)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Error"))
                .andExpect(jsonPath("$.errors.username").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());

        verify(registerUseCase, never()).register(any());
    }

    @Test
    @DisplayName("POST /api/auth/login: Đăng nhập thành công trả về 200 OK kèm cặp token")
    void login_Success() throws Exception {
        LoginCommand command = LoginCommand.builder()
                .usernameOrEmail("hoang_dev")
                .password("password123")
                .build();

        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken("access.jwt.token")
                .refreshToken("refresh.uuid.token")
                .tokenType("Bearer")
                .expiresIn(86400L)
                .build();

        when(loginUseCase.login(any(LoginCommand.class))).thenReturn(tokenResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access.jwt.token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh.uuid.token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(86400L));

        verify(loginUseCase, times(1)).login(any(LoginCommand.class));
    }

    @Test
    @DisplayName("POST /api/auth/login: Thất bại khi để trống username hoặc password trả về 400")
    void login_Fails_WhenFieldsBlank() throws Exception {
        LoginCommand command = LoginCommand.builder()
                .usernameOrEmail("")
                .password("")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Error"));

        verify(loginUseCase, never()).login(any());
    }

    @Test
    @DisplayName("POST /api/auth/refresh: Cấp mới token thành công trả về 200 OK")
    void refresh_Success() throws Exception {
        RefreshTokenCommand command = new RefreshTokenCommand("valid.refresh.token");

        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken("new.access.token")
                .refreshToken("new.refresh.token")
                .tokenType("Bearer")
                .expiresIn(86400L)
                .build();

        when(refreshTokenUseCase.refreshToken(any(RefreshTokenCommand.class))).thenReturn(tokenResponse);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new.access.token"))
                .andExpect(jsonPath("$.refreshToken").value("new.refresh.token"));

        verify(refreshTokenUseCase, times(1)).refreshToken(any(RefreshTokenCommand.class));
    }

    @Test
    @DisplayName("POST /api/auth/logout: Đăng xuất thành công trả về 200 OK")
    void logout_Success() throws Exception {
        LogoutCommand command = new LogoutCommand("refresh.token.to.revoke");

        doNothing().when(refreshTokenUseCase).logout(any(LogoutCommand.class));

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        verify(refreshTokenUseCase, times(1)).logout(any(LogoutCommand.class));
    }
}

