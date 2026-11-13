package com.hoang.ddd.application.service.event.Impl;

import com.hoang.ddd.application.service.event.EventAppService;
import com.hoang.ddd.domain.service.HiDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventAppServiceImpl implements EventAppService {

    private final HiDomainService hiDomainService;

    @Override
    public String sayHi(String hi) {
        return hiDomainService.sayHi(hi);
    }
}
