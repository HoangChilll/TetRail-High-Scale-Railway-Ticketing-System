package com.hoang.controller;

import com.hoang.dto.response.SeatAvailabilityResponse;
import com.hoang.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripTicketController {

    private final TicketService ticketService;

    /**
     * Lấy sơ đồ ghế và trạng thái vé của chuyến tàu (AVAILABLE, RESERVED, ISSUED)
     */
    @GetMapping("/{tripId}/tickets")
    public ResponseEntity<List<SeatAvailabilityResponse>> getTripTickets(@PathVariable("tripId") Long tripId) {
        List<SeatAvailabilityResponse> tickets = ticketService.getTripTickets(tripId);
        return ResponseEntity.ok(tickets);
    }
}

