package com.hoang.ddd.infrastructure.persistence.repository;

import com.hoang.ddd.domain.model.entity.Station;
import com.hoang.ddd.domain.repository.StationRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaStationRepository extends JpaRepository<Station, Long>, StationRepository {

    @Override
    Optional<Station> findByCode(String code);

    @Override
    boolean existsByCode(String code);
}
