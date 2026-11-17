package com.hoang.repository;

import com.hoang.model.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByTrainId(Long trainId);

    Optional<Seat> findByTrainIdAndSeatNumber(Long trainId, String seatNumber);

    List<Seat> findByTrainIdAndSeatClass(Long trainId, String seatClass);

    boolean existsByTrainIdAndSeatNumber(Long trainId, String seatNumber);
}
