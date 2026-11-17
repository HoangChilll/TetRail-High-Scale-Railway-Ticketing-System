package com.hoang.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoang.dto.request.LoginRequest;
import com.hoang.dto.request.LogoutRequest;
import com.hoang.dto.request.RefreshTokenRequest;
import com.hoang.dto.request.RegisterRequest;
import com.hoang.dto.response.TokenResponse;
import com.hoang.dto.response.UserResponse;
import com.hoang.service.AuthService;
import com.hoang.exception.GlobalExceptionHandler;
import com.hoang.model.Role;
import com.hoang.model.User;
import com.hoang.model.UserStatus;
import com.hoang.security.UserDetailsCustom;
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

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.hoang.security.JwtTokenProvider jwtTokenProvider;

    @org.springframework.boot.test.mock.mockito.MockBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    @DisplayName("POST /api/auth/register: Đăng ký thành công trả về 201 Created")
    void register_Success() throws Exception {
        RegisterRequest command = RegisterRequest.builder()
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

        when(authService.register(any(RegisterRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.username").value("hoang_dev"))
                .andExpect(jsonPath("$.email").value("hoang@tetrail.vn"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));

        verify(authService, times(1)).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("POST /api/auth/register: Thất bại khi dữ liệu không hợp lệ trả về 400 Bad Request")
    void register_Fails_WhenValidationErrors() throws Exception {
        // Dữ liệu lỗi: username quá ngắn, email sai định dạng, password trống
        RegisterRequest invalidCommand = RegisterRequest.builder()
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

        verify(authService, never()).register(any());
    }

    @Test
    @DisplayName("POST /api/auth/login: Đăng nhập thành công trả về 200 OK kèm cặp token")
    void login_Success() throws Exception {
        LoginRequest command = LoginRequest.builder()
                .usernameOrEmail("hoang_dev")
                .password("password123")
                .build();

        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken("access.jwt.token")
                .refreshToken("refresh.uuid.token")
                .tokenType("Bearer")
                .expiresIn(86400L)
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(tokenResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access.jwt.token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh.uuid.token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(86400L));

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("POST /api/auth/login: Thất bại khi để trống username hoặc password trả về 400")
    void login_Fails_WhenFieldsBlank() throws Exception {
        LoginRequest command = LoginRequest.builder()
                .usernameOrEmail("")
                .password("")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Error"));

        verify(authService, never()).login(any());
    }

    @Test
    @DisplayName("POST /api/auth/refresh: Cấp mới token thành công trả về 200 OK")
    void refresh_Success() throws Exception {
        RefreshTokenRequest command = new RefreshTokenRequest("valid.refresh.token");

        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken("new.access.token")
                .refreshToken("new.refresh.token")
                .tokenType("Bearer")
                .expiresIn(86400L)
                .build();

        when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(tokenResponse);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new.access.token"))
                .andExpect(jsonPath("$.refreshToken").value("new.refresh.token"));

        verify(authService, times(1)).refreshToken(any(RefreshTokenRequest.class));
    }

    @Test
    @DisplayName("POST /api/auth/logout: Đăng xuất thành công trả về 200 OK")
    void logout_Success() throws Exception {
        LogoutRequest command = new LogoutRequest("refresh.token.to.revoke");

        doNothing().when(authService).logout(any(LogoutRequest.class));

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        verify(authService, times(1)).logout(any(LogoutRequest.class));
    }
}

