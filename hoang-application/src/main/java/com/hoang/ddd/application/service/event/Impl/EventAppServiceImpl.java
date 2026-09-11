package com.hoang.ddd.application.service.event.Impl;

import com.hoang.ddd.application.service.event.EventAppService;
import com.hoang.ddd.domain.service.HiDomainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EventAppServiceImpl implements EventAppService {
    // call domain service
    @Autowired
    private HiDomainService hiDomainService;
    @Override
    public String sayHi(String hi){
        return hiDomainService.sayHi(hi);
    }

}
