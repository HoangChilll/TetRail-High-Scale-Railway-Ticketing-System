package com.hoang.ddd.infrastructure.persistence.repository;

import com.hoang.ddd.domain.model.entity.Booking;
import com.hoang.ddd.domain.model.entity.BookingStatus;
import com.hoang.ddd.domain.repository.BookingRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface JpaBookingRepository extends JpaRepository<Booking, Long>, BookingRepository {

    @Override
    Optional<Booking> findByBookingCode(String bookingCode);

    @Override
    List<Booking> findByUserId(Long userId);

    @Override
    List<Booking> findByStatusAndExpiresAtBefore(BookingStatus status, Instant dateTime);

    @Override
    boolean existsByBookingCode(String bookingCode);
}
