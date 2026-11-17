package com.hoang.scheduler;

import com.hoang.model.Booking;
import com.hoang.model.BookingStatus;
import com.hoang.repository.BookingRepository;
import com.hoang.repository.TicketRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class BookingExpirationSchedulerTest {
    @Test
    void cancelsExpiredBookingsAndUsesSameTimeToReleaseTickets() {
        Instant now = Instant.parse("2026-11-13T12:00:00Z");
        BookingRepository bookings = mock(BookingRepository.class);
        TicketRepository tickets = mock(TicketRepository.class);
        Booking booking = Booking.builder().id(7L).status(BookingStatus.PENDING).build();
        when(bookings.findByStatusAndExpiresAtBefore(BookingStatus.PENDING, now))
                .thenReturn(List.of(booking));
        BookingExpirationScheduler scheduler = new BookingExpirationScheduler(
                bookings, tickets, Clock.fixed(now, ZoneOffset.UTC));

        scheduler.releaseExpiredBookings();

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.getCancelledAt()).isEqualTo(now);
        verify(bookings).save(booking);
        verify(tickets).releaseTickets(7L, now);
    }
}
