package com.hoang.ddd.domain.repository;

import com.hoang.ddd.domain.model.entity.Booking;
import com.hoang.ddd.domain.model.entity.BookingStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface BookingRepository {

    Optional<Booking> findById(Long id);

    Optional<Booking> findByBookingCode(String bookingCode);

    List<Booking> findByUserId(Long userId);

    List<Booking> findByStatusAndExpiresAtBefore(BookingStatus status, Instant dateTime);

    boolean existsByBookingCode(String bookingCode);

    Booking save(Booking booking);
}
