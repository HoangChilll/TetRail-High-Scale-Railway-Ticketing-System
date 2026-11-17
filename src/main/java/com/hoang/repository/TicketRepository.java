package com.hoang.repository;

import com.hoang.model.Ticket;
import com.hoang.model.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findByTicketCode(String ticketCode);

    List<Ticket> findByIdIn(List<Long> ids);

    List<Ticket> findByBookingId(Long bookingId);

    List<Ticket> findByTripId(Long tripId);

    Page<Ticket> findByTripId(Long tripId, Pageable pageable);

    Page<Ticket> findByStatus(TicketStatus status, Pageable pageable);

    Optional<Ticket> findByTripIdAndSeatId(Long tripId, Long seatId);

    List<Ticket> findByTripIdAndStatus(Long tripId, TicketStatus status);

    boolean existsByTicketCode(String ticketCode);

    @Modifying(clearAutomatically = true)
    @Query("""
                UPDATE Ticket t
                SET t.booking.id = :bookingId,
                    t.passengerName = :passengerName,
                    t.status = com.hoang.model.TicketStatus.RESERVED,
                    t.updatedAt = :updatedAt
                WHERE t.id IN :ticketIds AND t.status = com.hoang.model.TicketStatus.AVAILABLE
            """)
    int reserveTickets(@Param("ticketIds") List<Long> ticketIds,
            @Param("bookingId") Long bookingId,
            @Param("passengerName") String passengerName,
            @Param("updatedAt") Instant updatedAt);

    // 2. Xuất vé sau khi thanh toán thành công
    @Modifying(clearAutomatically = true)
    @Query("""
                UPDATE Ticket t
                SET t.status = com.hoang.model.TicketStatus.ISSUED,
                    t.issuedAt = :issuedAt,
                    t.updatedAt = :issuedAt
                WHERE t.booking.id = :bookingId AND t.status = com.hoang.model.TicketStatus.RESERVED
            """)
    int issueTickets(@Param("bookingId") Long bookingId, @Param("issuedAt") Instant issuedAt);

    // 3. Nhả vé về lại kho nếu đơn hàng hết hạn 10 phút
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
                UPDATE Ticket t
                SET t.booking = null,
                    t.passengerName = null,
                    t.status = com.hoang.model.TicketStatus.AVAILABLE,
                    t.updatedAt = :updatedAt
                WHERE t.booking.id = :bookingId AND t.status = com.hoang.model.TicketStatus.RESERVED
            """)
    int releaseTickets(@Param("bookingId") Long bookingId, @Param("updatedAt") Instant updatedAt);
}
