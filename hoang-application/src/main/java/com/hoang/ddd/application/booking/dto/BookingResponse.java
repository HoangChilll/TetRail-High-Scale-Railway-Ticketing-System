package com.hoang.ddd.application.booking.dto;

import com.hoang.ddd.domain.model.entity.Booking;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookingResponse {
    private Long id;
    private String bookingCode;
    private String buyerName;
    private String buyerEmail;
    private String buyerPhone;
    private BigDecimal totalAmount;
    private String currency;
    private String status;
    private Instant expiresAt;
    private Instant createdAt;

    public static BookingResponse of(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .bookingCode(booking.getBookingCode())
                .buyerName(booking.getBuyerName())
                .buyerEmail(booking.getBuyerEmail())
                .buyerPhone(booking.getBuyerPhone())
                .totalAmount(booking.getTotalAmount())
                .currency(booking.getCurrency())
                .status(booking.getStatus().name())
                .expiresAt(booking.getExpiresAt())
                .createdAt(booking.getCreatedAt())
                .build();
    }
}

