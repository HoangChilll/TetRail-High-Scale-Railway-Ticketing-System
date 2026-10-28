package com.hoang.ddd.domain.repository;

import com.hoang.ddd.domain.model.entity.Ticket;
import com.hoang.ddd.domain.model.entity.TicketStatus;

import java.util.List;
import java.util.Optional;

public interface TicketRepository {

    Optional<Ticket> findById(Long id);

    Optional<Ticket> findByTicketCode(String ticketCode);

    List<Ticket> findByBookingId(Long bookingId);

    List<Ticket> findByTripId(Long tripId);

    Optional<Ticket> findByTripIdAndSeatId(Long tripId, Long seatId);

    List<Ticket> findByTripIdAndStatus(Long tripId, TicketStatus status);

    boolean existsByTicketCode(String ticketCode);

    Ticket save(Ticket ticket);
}
