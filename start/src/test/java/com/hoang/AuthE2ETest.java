package com.hoang;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoang.ddd.application.auth.dto.LoginCommand;
import com.hoang.ddd.application.auth.dto.LogoutCommand;
import com.hoang.ddd.application.auth.dto.RefreshTokenCommand;
import com.hoang.ddd.application.auth.dto.RegisterCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JavaMailSender javaMailSender;

    private static String accessToken;
    private static String refreshToken;
    private static String rotatedRefreshToken;

    @Test
    @Order(1)
    @DisplayName("E2E Bước 1: Đăng ký người dùng mới qua POST /api/auth/register")
    void step1_register() throws Exception {
        RegisterCommand command = RegisterCommand.builder()
                .username("e2e_tester")
                .email("e2e_tester@tetrail.vn")
                .password("SecureP@ssword123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.username").value("e2e_tester"))
                .andExpect(jsonPath("$.email").value("e2e_tester@tetrail.vn"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));
    }

    @Test
    @Order(2)
    @DisplayName("E2E Bước 2: Đăng nhập bằng tài khoản vừa tạo qua POST /api/auth/login")
    void step2_login() throws Exception {
        LoginCommand command = LoginCommand.builder()
                .usernameOrEmail("e2e_tester")
                .password("SecureP@ssword123")
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        accessToken = jsonNode.get("accessToken").asText();
        refreshToken = jsonNode.get("refreshToken").asText();

        assertNotNull(accessToken);
        assertNotNull(refreshToken);
    }

    @Test
    @Order(3)
    @DisplayName("E2E Bước 3: Truy cập endpoint bảo vệ GET /api/auth/me với Bearer Token")
    void step3_getCurrentUser_WithBearerToken() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("e2e_tester"))
                .andExpect(jsonPath("$.email").value("e2e_tester@tetrail.vn"));
    }

    @Test
    @Order(4)
    @DisplayName("E2E Bước 4: Thử truy cập GET /api/auth/me mà không có Token -> Bị từ chối 401")
    void step4_getCurrentUser_WithoutToken_Fails() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(5)
    @DisplayName("E2E Bước 5: Làm mới Token qua POST /api/auth/refresh (Token Rotation)")
    void step5_refreshToken() throws Exception {
        RefreshTokenCommand command = new RefreshTokenCommand(refreshToken);

        MvcResult result = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        String newAccessToken = jsonNode.get("accessToken").asText();
        rotatedRefreshToken = jsonNode.get("refreshToken").asText();

        assertNotNull(newAccessToken);
        assertNotNull(rotatedRefreshToken);
        // Refresh token mới phải khác với refresh token ban đầu
        assertNotEquals(refreshToken, rotatedRefreshToken);
    }

    @Test
    @Order(6)
    @DisplayName("E2E Bước 6: Đăng xuất người dùng qua POST /api/auth/logout")
    void step6_logout() throws Exception {
        LogoutCommand command = new LogoutCommand(rotatedRefreshToken);

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }

    @Test
    @Order(7)
    @DisplayName("E2E Bước 7: Dùng lại Refresh Token đã đăng xuất -> Bị từ chối 401")
    void step7_useRevokedRefreshToken_Fails() throws Exception {
        RefreshTokenCommand command = new RefreshTokenCommand(rotatedRefreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isUnauthorized());
    }
}

