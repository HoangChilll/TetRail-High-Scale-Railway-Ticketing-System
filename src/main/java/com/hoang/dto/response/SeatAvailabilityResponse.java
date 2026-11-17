package com.hoang.dto.response;

import com.hoang.model.Ticket;
import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatAvailabilityResponse {
    private Long ticketId;
    private Long seatId;
    private String seatNumber;
    private String carriageNumber;
    private String seatClass;
    private BigDecimal fare;
    private String status;

    public static SeatAvailabilityResponse of(Ticket ticket) {
        return SeatAvailabilityResponse.builder()
                .ticketId(ticket.getId())
                .seatId(ticket.getSeat().getId())
                .seatNumber(ticket.getSeat().getSeatNumber())
                .carriageNumber(ticket.getSeat().getCarriageNumber())
                .seatClass(ticket.getSeat().getSeatClass())
                .fare(ticket.getFare())
                .status(ticket.getStatus().name())
                .build();
    }
}
