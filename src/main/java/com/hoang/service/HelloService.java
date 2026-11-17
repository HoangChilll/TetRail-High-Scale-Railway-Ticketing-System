package com.hoang.service;

import org.springframework.stereotype.Service;

@Service
public class HelloService {
    public String sayHi(String who) {
        return "hi infrast";
    }
}
