package com.hoang.ddd.domain.service.impl;

import com.hoang.ddd.domain.repository.HiDomainRepository;
import com.hoang.ddd.domain.service.HiDomainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HiDomainServiceImpl implements HiDomainService {
    @Autowired
    private HiDomainRepository hiDomainRepository;
    @Override
    public String sayHi(String who){
        return hiDomainRepository.sayHi(who);
    }
}
