package com.hoang.ddd.infrastructure.persistence.repository;

import com.hoang.ddd.domain.model.entity.Ticket;
import com.hoang.ddd.domain.model.entity.TicketStatus;
import com.hoang.ddd.domain.repository.TicketRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaTicketRepository extends JpaRepository<Ticket, Long>, TicketRepository {

    @Override
    Optional<Ticket> findByTicketCode(String ticketCode);

    @Override
    List<Ticket> findByBookingId(Long bookingId);

    @Override
    List<Ticket> findByTripId(Long tripId);

    @Override
    Optional<Ticket> findByTripIdAndSeatId(Long tripId, Long seatId);

    @Override
    List<Ticket> findByTripIdAndStatus(Long tripId, TicketStatus status);

    @Override
    boolean existsByTicketCode(String ticketCode);
}
