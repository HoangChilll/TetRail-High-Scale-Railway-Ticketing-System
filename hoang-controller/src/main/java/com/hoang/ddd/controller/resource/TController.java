package com.hoang.ddd.controller.resource;

import com.hoang.ddd.application.service.event.EventAppService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/Hello")
public class TController {

    @Autowired
    private EventAppService eventAppService;

    @GetMapping("/hi")
    @RateLimiter(name = "backendA", fallbackMethod = "fallbackHello")
    public String hello() {
        return eventAppService.sayHi("hi");
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
