package com.hoang.ddd.domain.service.impl;

import com.hoang.ddd.domain.repository.HiDomainRepository;
import com.hoang.ddd.domain.service.HiDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HiDomainServiceImpl implements HiDomainService {
    private final HiDomainRepository hiDomainRepository;
    @Override
    public String sayHi(String who){
        return hiDomainRepository.sayHi(who);
    }
}
