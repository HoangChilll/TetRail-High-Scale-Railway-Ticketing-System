package com.hoang.ddd.controller.booking;

import com.hoang.ddd.application.booking.dto.response.SeatAvailabilityResponse;
import com.hoang.ddd.application.booking.usecase.GetTripTicketsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripTicketController {

    private final GetTripTicketsUseCase getTripTicketsUseCase;

    /**
     * Lấy sơ đồ ghế và trạng thái vé của chuyến tàu (AVAILABLE, RESERVED, ISSUED)
     */
    @GetMapping("/{tripId}/tickets")
    public ResponseEntity<List<SeatAvailabilityResponse>> getTripTickets(@PathVariable("tripId") Long tripId) {
        List<SeatAvailabilityResponse> tickets = getTripTicketsUseCase.execute(tripId);
        return ResponseEntity.ok(tickets);
    }
}

