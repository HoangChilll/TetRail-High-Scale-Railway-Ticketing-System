package com.hoang.ddd.application.booking.usecase;

import com.hoang.ddd.application.booking.dto.response.BookingResponse;
import com.hoang.ddd.application.booking.dto.command.CreateBookingCommand;
import com.hoang.ddd.application.booking.exception.TicketUnavailableException;
import com.hoang.ddd.domain.model.entity.Booking;
import com.hoang.ddd.domain.model.entity.BookingStatus;
import com.hoang.ddd.domain.model.entity.Ticket;
import com.hoang.ddd.domain.model.entity.User;
import com.hoang.ddd.domain.repository.BookingRepository;
import com.hoang.ddd.domain.repository.TicketRepository;
import com.hoang.ddd.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookTicketUseCase {

    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @CacheEvict(value = "trip_tickets", allEntries = true)
    @Transactional
    public BookingResponse execute(CreateBookingCommand command, Long userId) {
        List<Long> ticketIds = command.getTicketIds();

        // 1. Kiểm tra tồn tại vé và tính tổng tiền
        List<Ticket> tickets = ticketRepository.findByIdIn(ticketIds);
        if (tickets.size() != ticketIds.size()) {
            throw new IllegalArgumentException("Một số vé không tồn tại trong hệ thống!");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Ticket ticket : tickets) {
            totalAmount = totalAmount.add(ticket.getFare());
        }

        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId).orElse(null);
        }

        // 2. Tạo đơn Booking (PENDING, giữ chỗ trong 10 phút)
        String bookingCode = "BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Instant now = Instant.now(clock);
        Booking booking = Booking.builder()
                .bookingCode(bookingCode)
                .user(user)
                .buyerName(command.getBuyerName())
                .buyerEmail(command.getBuyerEmail())
                .buyerPhone(command.getBuyerPhone())
                .totalAmount(totalAmount)
                .currency("VND")
                .status(BookingStatus.PENDING)
                .expiresAt(now.plus(10, ChronoUnit.MINUTES))
                .build();

        Booking savedBooking = bookingRepository.save(booking);

        // 3. ATOMIC UPDATE: Thực hiện lệnh UPDATE giữ chỗ trong database
        int updatedCount = ticketRepository.reserveTickets(
                ticketIds,
                savedBooking.getId(),
                command.getBuyerName(),
                now);

        // 4. Nếu số vé update được < số vé yêu cầu -> Đã có người hớt tay trên
        if (updatedCount != ticketIds.size()) {
            throw new TicketUnavailableException();
        }

        return BookingResponse.of(savedBooking);
    }
}
