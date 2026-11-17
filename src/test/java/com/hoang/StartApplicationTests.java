package com.hoang;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class StartApplicationTests {

    @MockBean
    private JavaMailSender javaMailSender;

    @Test
    @DisplayName("Khởi tạo ApplicationContext thành công: Nạp đầy đủ Beans, Security và Cấu hình")
    void contextLoads() {
        // Kiểm tra toàn bộ Spring Boot ApplicationContext khởi động thành công
    }
}

