package com.hoang.ddd.application.usecase;

import com.hoang.ddd.application.booking.dto.BookingResponse;
import com.hoang.ddd.application.booking.dto.CreateBookingCommand;
import com.hoang.ddd.domain.model.entity.Booking;
import com.hoang.ddd.domain.model.entity.BookingStatus;
import com.hoang.ddd.domain.model.entity.Ticket;
import com.hoang.ddd.domain.repository.BookingRepository;
import com.hoang.ddd.domain.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookTicketUseCase {

    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;

    @Transactional
    public BookingResponse execute(CreateBookingCommand command, Long userId) {
        List<Long> ticketIds = command.getTicketIds();

        // 1. Kiểm tra giá vé để tính tổng tiền
        List<Ticket> tickets = ticketRepository.findByIdIn(ticketIds);
        if (tickets.size() != ticketIds.size()) {
            throw new IllegalArgumentException("Một số vé không tồn tại trong hệ thống!");
        }

        BigDecimal totalAmount = tickets.stream()
                .map(Ticket::getFare)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. Tạo bản ghi đơn Booking (PENDING, giữ chỗ trong 10 phút)
        String bookingCode = "BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Booking booking = Booking.builder()
                .bookingCode(bookingCode)
                .buyerName(command.getBuyerName())
                .buyerEmail(command.getBuyerEmail())
                .buyerPhone(command.getBuyerPhone())
                .totalAmount(totalAmount)
                .status(BookingStatus.PENDING)
                .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .build();

        Booking savedBooking = bookingRepository.save(booking);

        // 3. ATOMIC UPDATE: Thực hiện lệnh UPDATE duy nhất xuống DB
        int updatedCount = ticketRepository.reserveTickets(
                ticketIds,
                savedBooking.getId(),
                command.getBuyerName());

        // 4. Nếu số vé update được < số vé yêu cầu -> Đã có người hớt tay trên
        if (updatedCount < ticketIds.size()) {
            // Ném ngoại lệ để @Transactional tự động ROLLBACK hoàn toàn đơn Booking vừa tạo
            throw new IllegalStateException("Một số vé bạn chọn vừa có người khác giữ chỗ. Vui lòng chọn vé khác!");
        }

        return BookingResponse.of(savedBooking);
    }
}