package com.hoang.ddd.infrastructure.persistence.repository;

import com.hoang.ddd.domain.model.entity.Role;
import com.hoang.ddd.domain.repository.RoleRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaRoleRepository extends JpaRepository<Role, Long>, RoleRepository {

    @Override
    Optional<Role> findByName(String name);
}
