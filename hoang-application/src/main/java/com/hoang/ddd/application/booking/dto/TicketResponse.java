package com.hoang.ddd.application.booking.dto;

import com.hoang.ddd.domain.model.entity.Ticket;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TicketResponse {
    private Long id;
    private String ticketCode;
    private Long bookingId;
    private Long tripId;
    private Long seatId;
    private String seatNumber;
    private String carriageNumber;
    private String seatClass;
    private String passengerName;
    private String passengerId;
    private String passengerType;
    private Long departureStationId;
    private String departureStationName;
    private Long arrivalStationId;
    private String arrivalStationName;
    private BigDecimal fare;
    private String status;
    private Instant issuedAt;
    private Instant cancelledAt;
    private Instant createdAt;

    public static TicketResponse of(Ticket ticket) {
        if (ticket == null) {
            return null;
        }
        return TicketResponse.builder()
                .id(ticket.getId())
                .ticketCode(ticket.getTicketCode())
                .bookingId(ticket.getBooking() != null ? ticket.getBooking().getId() : null)
                .tripId(ticket.getTrip() != null ? ticket.getTrip().getId() : null)
                .seatId(ticket.getSeat() != null ? ticket.getSeat().getId() : null)
                .seatNumber(ticket.getSeat() != null ? ticket.getSeat().getSeatNumber() : null)
                .carriageNumber(ticket.getSeat() != null ? ticket.getSeat().getCarriageNumber() : null)
                .seatClass(ticket.getSeat() != null ? ticket.getSeat().getSeatClass() : null)
                .passengerName(ticket.getPassengerName())
                .passengerId(ticket.getPassengerId())
                .passengerType(ticket.getPassengerType())
                .departureStationId(ticket.getDepartureStation() != null ? ticket.getDepartureStation().getId() : null)
                .departureStationName(ticket.getDepartureStation() != null ? ticket.getDepartureStation().getName() : null)
                .arrivalStationId(ticket.getArrivalStation() != null ? ticket.getArrivalStation().getId() : null)
                .arrivalStationName(ticket.getArrivalStation() != null ? ticket.getArrivalStation().getName() : null)
                .fare(ticket.getFare())
                .status(ticket.getStatus() != null ? ticket.getStatus().name() : null)
                .issuedAt(ticket.getIssuedAt())
                .cancelledAt(ticket.getCancelledAt())
                .createdAt(ticket.getCreatedAt())
                .build();
    }
}
