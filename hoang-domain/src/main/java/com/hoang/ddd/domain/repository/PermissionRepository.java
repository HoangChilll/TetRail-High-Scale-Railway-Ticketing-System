package com.hoang.ddd.domain.repository;

import com.hoang.ddd.domain.model.entity.Permission;

import java.util.Optional;

public interface PermissionRepository {

    Optional<Permission> findById(Long id);

    Optional<Permission> findByName(String name);

    boolean existsByName(String name);

    Permission save(Permission permission);
}
