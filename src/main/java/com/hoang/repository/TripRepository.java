package com.hoang.repository;

import com.hoang.model.Trip;
import com.hoang.model.TripStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {

    List<Trip> findByDepartureStationIdAndArrivalStationIdAndDepartureTimeBetween(
            Long departureStationId,
            Long arrivalStationId,
            Instant departureTimeAfter,
            Instant departureTimeBefore
    );

    List<Trip> findByTrainIdAndDepartureTimeBetween(
            Long trainId,
            Instant departureTimeAfter,
            Instant departureTimeBefore
    );

    List<Trip> findByStatusAndDepartureTimeBetween(
            TripStatus status,
            Instant departureTimeAfter,
            Instant departureTimeBefore
    );
}
