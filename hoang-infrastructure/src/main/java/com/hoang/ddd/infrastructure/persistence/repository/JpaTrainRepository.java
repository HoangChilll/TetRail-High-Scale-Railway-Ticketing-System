package com.hoang.ddd.infrastructure.persistence.repository;

import com.hoang.ddd.domain.model.entity.Train;
import com.hoang.ddd.domain.repository.TrainRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaTrainRepository extends JpaRepository<Train, Long>, TrainRepository {

    @Override
    Optional<Train> findByTrainNumber(String trainNumber);

    @Override
    boolean existsByTrainNumber(String trainNumber);
}
