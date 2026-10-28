package com.hoang.ddd.domain.repository;

import com.hoang.ddd.domain.model.entity.Trip;
import com.hoang.ddd.domain.model.entity.TripStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TripRepository {

    Optional<Trip> findById(Long id);

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

    Trip save(Trip trip);
}
