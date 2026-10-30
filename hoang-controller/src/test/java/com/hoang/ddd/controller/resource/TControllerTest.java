package com.hoang.ddd.controller.resource;

import com.hoang.ddd.application.service.event.EventAppService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TController.class)
@Import(TControllerTest.TestSecurityConfig.class)
class TControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventAppService eventAppService;

    @TestConfiguration
    @EnableWebSecurity
    @EnableMethodSecurity
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .build();
        }
    }

    @Test
    @DisplayName("GET /Hello/hi: Gọi thành công EventAppService")
    @WithMockUser
    void hello_Success() throws Exception {
        when(eventAppService.sayHi("hi")).thenReturn("hi infrast");

        mockMvc.perform(get("/Hello/hi"))
                .andExpect(status().isOk())
                .andExpect(content().string("hi infrast"));
    }

    @Test
    @DisplayName("GET /Hello/user-only: Người dùng có ROLE_USER truy cập thành công 200 OK")
    @WithMockUser(roles = "USER")
    void userEndpoint_WithRoleUser_Success() throws Exception {
        mockMvc.perform(get("/Hello/user-only"))
                .andExpect(status().isOk())
                .andExpect(content().string("SUCCESS: Ban co ROLE_USER va duoc phep truy cap vao day!"));
    }

    @Test
    @DisplayName("GET /Hello/user-only: Người dùng có ROLE_ADMIN bị từ chối 403 Forbidden")
    @WithMockUser(roles = "ADMIN")
    void userEndpoint_WithRoleAdmin_Forbidden() throws Exception {
        mockMvc.perform(get("/Hello/user-only"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /Hello/admin-only: Người dùng có ROLE_ADMIN truy cập thành công 200 OK")
    @WithMockUser(roles = "ADMIN")
    void adminEndpoint_WithRoleAdmin_Success() throws Exception {
        mockMvc.perform(get("/Hello/admin-only"))
                .andExpect(status().isOk())
                .andExpect(content().string("SUCCESS: Chuc mung Admin!"));
    }

    @Test
    @DisplayName("GET /Hello/admin-only: Người dùng có ROLE_USER bị từ chối 403 Forbidden")
    @WithMockUser(roles = "USER")
    void adminEndpoint_WithRoleUser_Forbidden() throws Exception {
        mockMvc.perform(get("/Hello/admin-only"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Fallback Method: Trả về thông báo khi bị quá tải rate limit")
    void fallbackHello_DirectCall() {
        TController controller = new TController();
        String result = controller.fallbackHello(new RuntimeException("Rate limit exceeded"));
        org.junit.jupiter.api.Assertions.assertEquals("too many request", result);
    }
}

