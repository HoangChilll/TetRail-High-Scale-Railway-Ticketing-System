package com.hoang.ddd.application.booking.dto;

import com.hoang.ddd.domain.model.entity.Ticket;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TicketResponse {
    private Long id;
    private String ticketCode;
    private Long seatId;
    private String seatNumber;
    private String carriageNumber;
    private String seatClass;
    private BigDecimal fare;
    private String status;

    public static TicketResponse of(Ticket ticket) {
        return TicketResponse.builder()
                .id(ticket.getId())
                .ticketCode(ticket.getTicketCode())
                .seatId(ticket.getSeat() != null ? ticket.getSeat().getId() : null)
                .seatNumber(ticket.getSeat() != null ? ticket.getSeat().getSeatNumber() : null)
                .carriageNumber(ticket.getSeat() != null ? ticket.getSeat().getCarriageNumber() : null)
                .seatClass(ticket.getSeat() != null ? ticket.getSeat().getSeatClass() : null)
                .fare(ticket.getFare())
                .status(ticket.getStatus().name())
                .build();
    }
}

