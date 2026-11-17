package com.hoang.scheduler;

import com.hoang.model.Booking;
import com.hoang.model.BookingStatus;
import com.hoang.repository.BookingRepository;
import com.hoang.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.Clock;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingExpirationScheduler {

    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;
    private final Clock clock;

    /**
     * Định kỳ mỗi 1 phút quét và hủy các đơn Booking PENDING đã quá hạn 10 phút,
     * đồng thời giải phóng vé về trạng thái AVAILABLE.
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void releaseExpiredBookings() {
        Instant now = Instant.now(clock);
        List<Booking> expiredBookings = bookingRepository.findByStatusAndExpiresAtBefore(
                BookingStatus.PENDING,
                now);

        if (!expiredBookings.isEmpty()) {
            log.info("Tìm thấy {} đơn đặt vé quá hạn cần giải phóng", expiredBookings.size());
        }

        for (Booking booking : expiredBookings) {
            log.info("Đang hủy đơn đặt vé quá hạn: {} (id={})", booking.getBookingCode(), booking.getId());

            // 1. Chuyển trạng thái booking thành CANCELLED
            booking.setStatus(BookingStatus.CANCELLED);
            booking.setCancelledAt(now);
            bookingRepository.save(booking);

            // 2. Atomic Update nhả các vé liên quan về AVAILABLE và gỡ bỏ booking_id
            int releasedCount = ticketRepository.releaseTickets(booking.getId(), now);
            log.info("Đã giải phóng {} vé của đơn {}", releasedCount, booking.getBookingCode());
        }
    }
}
