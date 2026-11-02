package com.hoang.ddd.domain.repository;

import com.hoang.ddd.domain.model.entity.Ticket;
import com.hoang.ddd.domain.model.entity.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface TicketRepository {

    Optional<Ticket> findById(Long id);

    Optional<Ticket> findByTicketCode(String ticketCode);

    List<Ticket> findByIdIn(List<Long> ids);

    List<Ticket> findByBookingId(Long bookingId);

    List<Ticket> findByTripId(Long tripId);

    Page<Ticket> findAll(Pageable pageable);

    Page<Ticket> findByTripId(Long tripId, Pageable pageable);

    Page<Ticket> findByStatus(TicketStatus status, Pageable pageable);

    Optional<Ticket> findByTripIdAndSeatId(Long tripId, Long seatId);

    List<Ticket> findByTripIdAndStatus(Long tripId, TicketStatus status);

    boolean existsByTicketCode(String ticketCode);

    Ticket save(Ticket ticket);

    <S extends Ticket> List<S> saveAll(Iterable<S> entities);

    // Atomic update: Giữ chỗ vé
    int reserveTickets(List<Long> ticketIds, Long bookingId, String passengerName);

    // Atomic update: Xác nhận phát hành vé sau khi thanh toán thành công
    int issueTickets(Long bookingId);

    // Atomic update: Nhả vé về AVAILABLE khi booking hết hạn/bị hủy
    int releaseTickets(Long bookingId);
}
