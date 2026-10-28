package com.hoang.ddd.domain.repository;

import com.hoang.ddd.domain.model.entity.Station;

import java.util.List;
import java.util.Optional;

public interface StationRepository {

    Optional<Station> findById(Long id);

    Optional<Station> findByCode(String code);

    boolean existsByCode(String code);

    List<Station> findAll();

    Station save(Station station);
}
