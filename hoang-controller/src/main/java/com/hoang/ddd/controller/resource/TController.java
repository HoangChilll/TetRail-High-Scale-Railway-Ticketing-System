package com.hoang.ddd.controller.resource;

import com.hoang.ddd.application.service.event.EventAppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/Hello")
public class TController{
    @Autowired
    private EventAppService eventAppService;
    @GetMapping("/hi")
    public String hello(){
        return eventAppService.sayHi("hi");
    }
}
