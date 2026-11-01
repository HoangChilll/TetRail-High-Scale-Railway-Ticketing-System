package com.hoang.ddd.application.trip.service;

import com.hoang.ddd.domain.model.entity.Seat;
import com.hoang.ddd.domain.model.entity.Ticket;
import com.hoang.ddd.domain.model.entity.TicketStatus;
import com.hoang.ddd.domain.model.entity.Trip;
import com.hoang.ddd.domain.repository.SeatRepository;
import com.hoang.ddd.domain.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TripTicketGeneratorService {

    private final SeatRepository seatRepository;
    private final TicketRepository ticketRepository;

    /**
     * Tự động sinh kho vé AVAILABLE cho một chuyến tàu vừa tạo
     */
    @Transactional
    public List<Ticket> generateTicketsForTrip(Trip trip, BigDecimal defaultFare) {
        List<Seat> seats = seatRepository.findByTrainId(trip.getTrain().getId());

        List<Ticket> preGeneratedTickets = new ArrayList<>();

        for (Seat seat : seats) {
            String ticketCode = String.format("TK-%d-%d", trip.getId(), seat.getId());

            Ticket ticket = Ticket.builder()
                    .ticketCode(ticketCode)
                    .trip(trip)
                    .seat(seat)
                    .departureStation(trip.getDepartureStation())
                    .arrivalStation(trip.getArrivalStation())
                    .fare(defaultFare)
                    .status(TicketStatus.AVAILABLE)
                    .booking(null)
                    .passengerName(null)
                    .build();

            preGeneratedTickets.add(ticket);
        }

        return ticketRepository.saveAll(preGeneratedTickets);
    }
}

