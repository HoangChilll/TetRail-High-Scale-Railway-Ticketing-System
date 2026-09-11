package com.hoang.ddd.infrastructure.persistence.repository;

import com.hoang.ddd.domain.repository.HiDomainRepository;
import org.springframework.stereotype.Service;

@Service
public class HiInfrasRepositoryImpl implements HiDomainRepository {
    @Override
    public String sayHi(String who) {
        return "hi infrast";
    }
}
