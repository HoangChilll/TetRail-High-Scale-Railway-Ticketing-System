package com.hoang.controller;

import com.hoang.dto.response.TicketResponse;
import com.hoang.service.TicketService;
import com.hoang.dto.response.PageResponse;
import com.hoang.model.TicketStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @GetMapping
    public ResponseEntity<PageResponse<TicketResponse>> getTickets(
            @PageableDefault(page = 0, size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(name = "tripId", required = false) Long tripId,
            @RequestParam(name = "status", required = false) TicketStatus status) {

        PageResponse<TicketResponse> response = ticketService.getTickets(pageable, tripId, status);
        return ResponseEntity.ok(response);
    }

    /**
     * Tra cứu thông tin chi tiết của một vé theo ID
     *
     * Ví dụ: GET /api/tickets/1
     */
    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> getTicketById(@PathVariable("id") Long id) {
        TicketResponse response = ticketService.getById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Tra cứu thông tin chi tiết của một vé theo Mã vé (Ticket Code)
     *
     * Ví dụ: GET /api/tickets/code/TK-20261102-001
     */
    @GetMapping("/code/{ticketCode}")
    public ResponseEntity<TicketResponse> getTicketByCode(@PathVariable("ticketCode") String ticketCode) {
        TicketResponse response = ticketService.getByCode(ticketCode);
        return ResponseEntity.ok(response);
    }
}
