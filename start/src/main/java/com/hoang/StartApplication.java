package com.hoang;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class StartApplication {

    public static void main(String[] args) {
        // Thiết lập múi giờ chuẩn tương thích hoàn toàn với PostgreSQL Docker
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SpringApplication.run(StartApplication.class, args);
    }
}
