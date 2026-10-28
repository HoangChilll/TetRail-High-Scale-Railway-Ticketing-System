package com.hoang.ddd.domain.repository;

import com.hoang.ddd.domain.model.entity.Train;

import java.util.List;
import java.util.Optional;

public interface TrainRepository {

    Optional<Train> findById(Long id);

    Optional<Train> findByTrainNumber(String trainNumber);

    boolean existsByTrainNumber(String trainNumber);

    List<Train> findAll();

    Train save(Train train);
}
