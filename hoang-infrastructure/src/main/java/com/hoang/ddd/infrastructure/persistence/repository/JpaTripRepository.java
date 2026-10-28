package com.hoang.ddd.infrastructure.persistence.repository;

import com.hoang.ddd.domain.model.entity.Trip;
import com.hoang.ddd.domain.model.entity.TripStatus;
import com.hoang.ddd.domain.repository.TripRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface JpaTripRepository extends JpaRepository<Trip, Long>, TripRepository {

    @Override
    List<Trip> findByDepartureStationIdAndArrivalStationIdAndDepartureTimeBetween(
            Long departureStationId,
            Long arrivalStationId,
            Instant departureTimeAfter,
            Instant departureTimeBefore
    );

    @Override
    List<Trip> findByTrainIdAndDepartureTimeBetween(
            Long trainId,
            Instant departureTimeAfter,
            Instant departureTimeBefore
    );

    @Override
    List<Trip> findByStatusAndDepartureTimeBetween(
            TripStatus status,
            Instant departureTimeAfter,
            Instant departureTimeBefore
    );
}
