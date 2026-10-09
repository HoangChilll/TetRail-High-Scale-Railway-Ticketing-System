package com.hoang.ddd.infrastructure.persistence.repository;

import com.hoang.ddd.domain.model.entity.Permission;
import com.hoang.ddd.domain.repository.PermissionRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaPermissionRepository extends JpaRepository<Permission, Long>, PermissionRepository {

    @Override
    Optional<Permission> findByName(String name);
}
