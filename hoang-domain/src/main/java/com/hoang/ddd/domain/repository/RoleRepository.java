package com.hoang.ddd.domain.repository;

import com.hoang.ddd.domain.model.entity.Role;

import java.util.Optional;

public interface RoleRepository {

    Optional<Role> findById(Long id);

    Optional<Role> findByName(String name);

    boolean existsByName(String name);

    Role save(Role role);
}
