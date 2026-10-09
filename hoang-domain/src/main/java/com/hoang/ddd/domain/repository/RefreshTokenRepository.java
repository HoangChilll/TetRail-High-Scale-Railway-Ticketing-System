package com.hoang.ddd.domain.repository;

import com.hoang.ddd.domain.model.entity.RefreshToken;
import com.hoang.ddd.domain.model.entity.User;

import java.util.Optional;

public interface RefreshTokenRepository {

    Optional<RefreshToken> findByToken(String token);

    RefreshToken save(RefreshToken refreshToken);

    void deleteByUser(User user);
}
