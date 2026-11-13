package com.hoang;

import com.hoang.ddd.application.booking.scheduler.BookingExpirationScheduler;
import com.hoang.ddd.domain.model.entity.*;
import com.hoang.ddd.infrastructure.persistence.repository.JpaTicketRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = "app.scheduling.enabled=false")
@ActiveProfiles("test")
class BookingExpirationIntegrationTest {
    private static final Instant NOW = Instant.parse("2026-11-13T12:00:00Z");
    @Autowired private BookingExpirationScheduler scheduler;
    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;
    @MockBean private JavaMailSender mailSender;
    @MockBean private Clock clock;
    @SpyBean private JpaTicketRepository tickets;
    private TransactionTemplate transactions;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(NOW);
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        transactions = new TransactionTemplate(transactionManager);
        transactions.executeWithoutResult(status -> {
            for (String entity : new String[]{"Ticket", "Booking", "Trip", "Seat", "Train", "Station"}) {
                entityManager.createQuery("delete from " + entity).executeUpdate();
            }
        });
    }

    @Test
    void releasesOnlyExpiredPendingBookingsAndOnlyReservedTickets() {
        Long expired = createBooking("expired", BookingStatus.PENDING, NOW.minusSeconds(1));
        Long future = createBooking("future", BookingStatus.PENDING, NOW.plusSeconds(1));
        Long boundary = createBooking("boundary", BookingStatus.PENDING, NOW);
        Long confirmed = createBooking("confirmed", BookingStatus.CONFIRMED, NOW.minusSeconds(1));
        Long reserved = createTicket("reserved", expired, TicketStatus.RESERVED);
        Long issued = createTicket("issued", expired, TicketStatus.ISSUED);

        scheduler.releaseExpiredBookings();

        transactions.executeWithoutResult(status -> {
            Booking booking = entityManager.find(Booking.class, expired);
            assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
            assertThat(booking.getCancelledAt()).isEqualTo(NOW);
            Ticket released = entityManager.find(Ticket.class, reserved);
            assertThat(released.getStatus()).isEqualTo(TicketStatus.AVAILABLE);
            assertThat(released.getBooking()).isNull();
            assertThat(released.getPassengerName()).isNull();
            assertThat(released.getUpdatedAt()).isEqualTo(NOW);
            assertThat(entityManager.find(Ticket.class, issued).getStatus()).isEqualTo(TicketStatus.ISSUED);
            assertThat(entityManager.find(Booking.class, future).getStatus()).isEqualTo(BookingStatus.PENDING);
            assertThat(entityManager.find(Booking.class, boundary).getStatus()).isEqualTo(BookingStatus.PENDING);
            assertThat(entityManager.find(Booking.class, confirmed).getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        });

        // Lần quét thứ hai không xử lý lại booking đã hủy.
        clearInvocations(tickets);
        scheduler.releaseExpiredBookings();
        verify(tickets, never()).releaseTickets(anyLong(), any());
    }

    @Test
    void rollsBackCancellationWhenReleasingTicketsFails() {
        Long bookingId = createBooking("rollback", BookingStatus.PENDING, NOW.minusSeconds(1));
        Long ticketId = createTicket("rollback", bookingId, TicketStatus.RESERVED);
        doThrow(new DataAccessResourceFailureException("Database failure"))
                .when(tickets).releaseTickets(eq(bookingId), any());

        assertThatThrownBy(() -> scheduler.releaseExpiredBookings())
                .isInstanceOf(DataAccessResourceFailureException.class);

        transactions.executeWithoutResult(status -> {
            Booking booking = entityManager.find(Booking.class, bookingId);
            assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);
            assertThat(booking.getCancelledAt()).isNull();
            Ticket ticket = entityManager.find(Ticket.class, ticketId);
            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.RESERVED);
            assertThat(ticket.getBooking().getId()).isEqualTo(bookingId);
        });
    }

    private Long createBooking(String code, BookingStatus status, Instant expiresAt) {
        return transactions.execute(transaction -> {
            Booking booking = Booking.builder().bookingCode(code).buyerName("Learner")
                    .buyerEmail("learner@example.com").buyerPhone("0900000000")
                    .totalAmount(BigDecimal.TEN).status(status).expiresAt(expiresAt).build();
            entityManager.persist(booking);
            return booking.getId();
        });
    }

    private Long createTicket(String code, Long bookingId, TicketStatus status) {
        return transactions.execute(transaction -> {
            Station departure = Station.builder().code("DEP-" + code).name("Departure").build();
            Station arrival = Station.builder().code("ARR-" + code).name("Arrival").build();
            Train train = Train.builder().trainNumber("TRAIN-" + code).build();
            entityManager.persist(departure);
            entityManager.persist(arrival);
            entityManager.persist(train);
            Seat seat = Seat.builder().train(train).seatNumber("1").carriageNumber("1").seatClass("STANDARD").build();
            Trip trip = Trip.builder().train(train).departureStation(departure).arrivalStation(arrival)
                    .departureTime(NOW.plusSeconds(3600)).arrivalTime(NOW.plusSeconds(7200)).build();
            entityManager.persist(seat);
            entityManager.persist(trip);
            Ticket ticket = Ticket.builder().ticketCode(code).booking(entityManager.find(Booking.class, bookingId))
                    .trip(trip).seat(seat).departureStation(departure).arrivalStation(arrival)
                    .fare(BigDecimal.TEN).status(status).passengerName("Learner").build();
            entityManager.persist(ticket);
            return ticket.getId();
        });
    }
}
