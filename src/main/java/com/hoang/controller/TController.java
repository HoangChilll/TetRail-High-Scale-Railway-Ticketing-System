package com.hoang.controller;

import com.hoang.service.HelloService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/Hello")
public class TController {

    private final HelloService helloService;

    @GetMapping("/hi")
    @RateLimiter(name = "backendA", fallbackMethod = "fallbackHello")
    public String hello() {
        return helloService.sayHi("hi");
    }

    public String fallbackHello(Throwable throwable) {
        return "too many request";
    }

    // 1. Endpoint chỉ dành cho người dùng có ROLE_USER
    @GetMapping("/user-only")
    @PreAuthorize("hasRole('USER')")
    public String userEndpoint() {
        return "SUCCESS: Ban co ROLE_USER va duoc phep truy cap vao day!";
    }

    // 2. Endpoint chỉ dành cho Admin có ROLE_ADMIN
    @GetMapping("/admin-only")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminEndpoint() {
        return "SUCCESS: Chuc mung Admin!";
    }
}
