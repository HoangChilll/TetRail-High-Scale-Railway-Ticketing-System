package com.hoang.ddd.application.booking.usecase;

import com.hoang.ddd.application.booking.dto.CreateBookingCommand;
import com.hoang.ddd.domain.model.entity.Booking;
import com.hoang.ddd.domain.model.entity.Ticket;
import com.hoang.ddd.domain.model.entity.User;
import com.hoang.ddd.domain.repository.BookingRepository;
import com.hoang.ddd.domain.repository.TicketRepository;
import com.hoang.ddd.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
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
class BookTicketUseCaseTest {
    private static final Instant NOW = Instant.parse("2026-11-05T00:00:00Z");

    @Mock private BookingRepository bookings;
    @Mock private TicketRepository tickets;
    @Mock private UserRepository users;

    private BookTicketUseCase useCase;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        useCase = new BookTicketUseCase(bookings, tickets, users, clock);
    }

    private CreateBookingCommand command() {
        return CreateBookingCommand.builder()
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

    private void assignBookingIdWhenSaved() {
        when(bookings.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            booking.setId(99L);
            return booking;
        });
    }

    @Test
    void createsPendingBookingWithCorrectAmountAndExpiry() {
        CreateBookingCommand command = command();
        when(tickets.findByIdIn(command.getTicketIds())).thenReturn(availableTickets());
        assignBookingIdWhenSaved();
        when(tickets.reserveTickets(command.getTicketIds(), 99L, command.getBuyerName(), NOW))
                .thenReturn(2);

        var result = useCase.execute(command, null);

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
    void doesNotSaveBookingWhenSomeTicketsAreMissing() {
        CreateBookingCommand command = command();
        when(tickets.findByIdIn(command.getTicketIds()))
                .thenReturn(List.of(availableTickets().getFirst()));

        assertThatThrownBy(() -> useCase.execute(command, null))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(bookings);
        verify(tickets, never()).reserveTickets(any(), any(), any(), any());
    }

    @Test
    void rejectsBookingWhenOnlyOneTicketCanBeReserved() {
        CreateBookingCommand command = command();
        when(tickets.findByIdIn(command.getTicketIds())).thenReturn(availableTickets());
        assignBookingIdWhenSaved();
        when(tickets.reserveTickets(command.getTicketIds(), 99L, command.getBuyerName(), NOW))
                .thenReturn(1);

        assertThatThrownBy(() -> useCase.execute(command, null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void attachesLoggedInUserToBooking() {
        CreateBookingCommand command = command();
        User user = User.builder().id(7L).build();
        when(tickets.findByIdIn(command.getTicketIds())).thenReturn(availableTickets());
        when(users.findById(7L)).thenReturn(Optional.of(user));
        assignBookingIdWhenSaved();
        when(tickets.reserveTickets(command.getTicketIds(), 99L, command.getBuyerName(), NOW))
                .thenReturn(2);

        useCase.execute(command, 7L);

        ArgumentCaptor<Booking> saved = ArgumentCaptor.forClass(Booking.class);
        verify(bookings).save(saved.capture());
        assertThat(saved.getValue().getUser()).isSameAs(user);
        verify(users).findById(eq(7L));
    }
}
