package com.hoang.ddd.application.booking.scheduler;

import com.hoang.ddd.domain.model.entity.Booking;
import com.hoang.ddd.domain.model.entity.BookingStatus;
import com.hoang.ddd.domain.repository.BookingRepository;
import com.hoang.ddd.domain.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingExpirationScheduler {

    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;

    /**
     * Định kỳ mỗi 1 phút quét và hủy các đơn Booking PENDING đã quá hạn 10 phút,
     * đồng thời giải phóng vé về trạng thái AVAILABLE.
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void releaseExpiredBookings() {
        List<Booking> expiredBookings = bookingRepository.findByStatusAndExpiresAtBefore(
                BookingStatus.PENDING,
                Instant.now());

        if (!expiredBookings.isEmpty()) {
            log.info("Tìm thấy {} đơn đặt vé quá hạn cần giải phóng", expiredBookings.size());
        }

        for (Booking booking : expiredBookings) {
            log.info("Đang hủy đơn đặt vé quá hạn: {} (id={})", booking.getBookingCode(), booking.getId());

            // 1. Chuyển trạng thái booking thành CANCELLED
            booking.setStatus(BookingStatus.CANCELLED);
            booking.setCancelledAt(Instant.now());
            bookingRepository.save(booking);

            // 2. Atomic Update nhả các vé liên quan về AVAILABLE và gỡ bỏ booking_id
            int releasedCount = ticketRepository.releaseTickets(booking.getId());
            log.info("Đã giải phóng {} vé của đơn {}", releasedCount, booking.getBookingCode());
        }
    }
}
