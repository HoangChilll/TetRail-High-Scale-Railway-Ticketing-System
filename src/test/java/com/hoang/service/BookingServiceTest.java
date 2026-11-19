package com.hoang.service;

import com.hoang.dto.request.CreateBookingRequest;
import com.hoang.exception.TicketUnavailableException;
import com.hoang.model.Booking;
import com.hoang.model.Ticket;
import com.hoang.model.User;
import com.hoang.repository.BookingRepository;
import com.hoang.repository.TicketRepository;
import com.hoang.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-05T00:00:00Z");

    @Mock private BookingRepository bookings;
    @Mock private TicketRepository tickets;
    @Mock private UserRepository users;

    private BookingService bookingService;
    //trước khi chạy test
    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        bookingService = new BookingService(bookings, tickets, users, clock);
    }
    // giả lập dữ liệu
    private CreateBookingRequest command() {
        return CreateBookingRequest.builder()
                .ticketIds(List.of(11L, 12L))
                .buyerName("Nguyen Van A")
                .buyerEmail("learner@example.com")
                .buyerPhone("0900000000")
                .build();
    }

    private List<Ticket> availableTickets() {
        return List.of(
                Ticket.builder().id(11L).fare(new BigDecimal("100000")).build(),
                Ticket.builder().id(12L).fare(new BigDecimal("150000")).build());
    }
    // giả lập việc đã lưu vô database
    private void assignBookingIdWhenSaved() {
        when(bookings.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            booking.setId(99L);
            return booking;
        });
    }

    @Test
    @DisplayName("Trường hợp tạo booking thành công")
    void createsPendingBookingWithCorrectAmountAndExpiry() {
        CreateBookingRequest command = command();
        when(tickets.findByIdIn(command.getTicketIds())).thenReturn(availableTickets());
        assignBookingIdWhenSaved();
        when(tickets.reserveTickets(command.getTicketIds(), 99L, command.getBuyerName(), NOW))
                .thenReturn(2);

        var result = bookingService.createBooking(command, null);

        assertThat(result.getId()).isEqualTo(99L);
        assertThat(result.getStatus()).isEqualTo("PENDING");
        assertThat(result.getTotalAmount()).isEqualByComparingTo("250000");
        assertThat(result.getExpiresAt()).isEqualTo(NOW.plusSeconds(600));
        ArgumentCaptor<Booking> saved = ArgumentCaptor.forClass(Booking.class);
        verify(bookings).save(saved.capture());
        assertThat(saved.getValue().getCurrency()).isEqualTo("VND");
        verify(tickets).reserveTickets(command.getTicketIds(), 99L, command.getBuyerName(), NOW);
    }

    @Test
    @DisplayName("Trường hợp khi mà người dùng mua 2 vé mà chỉ có 1 vé được phép mua")
    void doesNotSaveBookingWhenSomeTicketsAreMissing() {
        CreateBookingRequest command = command();
        when(tickets.findByIdIn(command.getTicketIds()))
                .thenReturn(List.of(availableTickets().getFirst()));

        assertThatThrownBy(() -> bookingService.createBooking(command, null))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(bookings);
        verify(tickets, never()).reserveTickets(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Trường hợp khi mà người dùng mua 2 vé mà database chỉ thay đổi trạng thái của 1 ticket")
    void rejectsBookingWhenOnlyOneTicketCanBeReserved() {
        CreateBookingRequest command = command();
        when(tickets.findByIdIn(command.getTicketIds())).thenReturn(availableTickets());
        assignBookingIdWhenSaved();
        when(tickets.reserveTickets(command.getTicketIds(), 99L, command.getBuyerName(), NOW))
                .thenReturn(1);

        assertThatThrownBy(() -> bookingService.createBooking(command, null))
                .isInstanceOf(TicketUnavailableException.class);
    }

    @Test
    @DisplayName("Trường hợp khi đúng của người mua cũng là người đặt vé")
    void attachesLoggedInUserToBooking() {
        CreateBookingRequest command = command();
        User user = User.builder().id(7L).build();
        when(tickets.findByIdIn(command.getTicketIds())).thenReturn(availableTickets());
        when(users.findById(7L)).thenReturn(Optional.of(user));
        assignBookingIdWhenSaved();
        when(tickets.reserveTickets(command.getTicketIds(), 99L, command.getBuyerName(), NOW))
                .thenReturn(2);

        bookingService.createBooking(command, 7L);

        ArgumentCaptor<Booking> saved = ArgumentCaptor.forClass(Booking.class);
        verify(bookings).save(saved.capture());
        assertThat(saved.getValue().getUser()).isSameAs(user);
        verify(users).findById(eq(7L));
    }
}
