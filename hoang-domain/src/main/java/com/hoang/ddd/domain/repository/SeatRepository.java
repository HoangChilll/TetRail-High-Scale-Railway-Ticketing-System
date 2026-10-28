package com.hoang.ddd.domain.repository;

import com.hoang.ddd.domain.model.entity.Seat;

import java.util.List;
import java.util.Optional;

public interface SeatRepository {

    Optional<Seat> findById(Long id);

    List<Seat> findByTrainId(Long trainId);

    Optional<Seat> findByTrainIdAndSeatNumber(Long trainId, String seatNumber);

    List<Seat> findByTrainIdAndSeatClass(Long trainId, String seatClass);

    boolean existsByTrainIdAndSeatNumber(Long trainId, String seatNumber);

    Seat save(Seat seat);
}
