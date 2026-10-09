package com.hoang.ddd.infrastructure.persistence.repository;

import com.hoang.ddd.domain.model.entity.User;
import com.hoang.ddd.domain.repository.UserRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaUserRepository extends JpaRepository<User, Long>, UserRepository {

    @Override
    Optional<User> findById(Long id);

    @Override
    Optional<User> findByUsername(String username);

    @Override
    Optional<User> findByEmail(String email);

    @Override
    Optional<User> findByUsernameOrEmail(String username, String email);

    @Override
    boolean existsByUsername(String username);

    @Override
    boolean existsByEmail(String email);
}
