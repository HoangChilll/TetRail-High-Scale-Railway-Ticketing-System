package com.hoang.ddd.infrastructure.persistence.repository;

import com.hoang.ddd.domain.model.entity.Seat;
import com.hoang.ddd.domain.repository.SeatRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaSeatRepository extends JpaRepository<Seat, Long>, SeatRepository {

    @Override
    List<Seat> findByTrainId(Long trainId);

    @Override
    Optional<Seat> findByTrainIdAndSeatNumber(Long trainId, String seatNumber);

    @Override
    List<Seat> findByTrainIdAndSeatClass(Long trainId, String seatClass);

    @Override
    boolean existsByTrainIdAndSeatNumber(Long trainId, String seatNumber);
}
