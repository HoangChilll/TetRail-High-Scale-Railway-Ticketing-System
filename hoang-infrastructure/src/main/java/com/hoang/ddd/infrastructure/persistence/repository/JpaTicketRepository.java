package com.hoang.ddd.infrastructure.persistence.repository;

import com.hoang.ddd.domain.model.entity.Ticket;
import com.hoang.ddd.domain.model.entity.TicketStatus;
import com.hoang.ddd.domain.repository.TicketRepository;
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
public interface JpaTicketRepository extends JpaRepository<Ticket, Long>, TicketRepository {

    @Override
    Optional<Ticket> findByTicketCode(String ticketCode);

    @Override
    List<Ticket> findByIdIn(List<Long> ids);

    @Override
    List<Ticket> findByBookingId(Long bookingId);

    @Override
    List<Ticket> findByTripId(Long tripId);

    @Override
    Page<Ticket> findByTripId(Long tripId, Pageable pageable);

    @Override
    Page<Ticket> findByStatus(TicketStatus status, Pageable pageable);

    @Override
    Optional<Ticket> findByTripIdAndSeatId(Long tripId, Long seatId);

    @Override
    List<Ticket> findByTripIdAndStatus(Long tripId, TicketStatus status);

    @Override
    boolean existsByTicketCode(String ticketCode);

    @Modifying(clearAutomatically = true)
    @Query("""
                UPDATE Ticket t
                SET t.booking.id = :bookingId,
                    t.passengerName = :passengerName,
                    t.status = com.hoang.ddd.domain.model.entity.TicketStatus.RESERVED,
                    t.updatedAt = :updatedAt
                WHERE t.id IN :ticketIds AND t.status = com.hoang.ddd.domain.model.entity.TicketStatus.AVAILABLE
            """)
    int reserveTickets(@Param("ticketIds") List<Long> ticketIds,
            @Param("bookingId") Long bookingId,
            @Param("passengerName") String passengerName,
            @Param("updatedAt") Instant updatedAt);

    // 2. Xuất vé sau khi thanh toán thành công
    @Modifying(clearAutomatically = true)
    @Query("""
                UPDATE Ticket t
                SET t.status = com.hoang.ddd.domain.model.entity.TicketStatus.ISSUED,
                    t.issuedAt = :issuedAt,
                    t.updatedAt = :issuedAt
                WHERE t.booking.id = :bookingId AND t.status = com.hoang.ddd.domain.model.entity.TicketStatus.RESERVED
            """)
    int issueTickets(@Param("bookingId") Long bookingId, @Param("issuedAt") Instant issuedAt);

    // 3. Nhả vé về lại kho nếu đơn hàng hết hạn 10 phút
    @Modifying(clearAutomatically = true)
    @Query("""
                UPDATE Ticket t
                SET t.booking = null,
                    t.passengerName = null,
                    t.status = com.hoang.ddd.domain.model.entity.TicketStatus.AVAILABLE,
                    t.updatedAt = :updatedAt
                WHERE t.booking.id = :bookingId AND t.status = com.hoang.ddd.domain.model.entity.TicketStatus.RESERVED
            """)
    int releaseTickets(@Param("bookingId") Long bookingId, @Param("updatedAt") Instant updatedAt);
}
