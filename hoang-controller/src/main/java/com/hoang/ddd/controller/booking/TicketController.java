package com.hoang.ddd.controller.booking;

import com.hoang.ddd.application.booking.dto.TicketResponse;
import com.hoang.ddd.application.booking.usecase.GetTicketByCodeUseCase;
import com.hoang.ddd.application.booking.usecase.GetTicketByIdUseCase;
import com.hoang.ddd.application.booking.usecase.GetTicketsPageUseCase;
import com.hoang.ddd.application.common.dto.PageResponse;
import com.hoang.ddd.domain.model.entity.TicketStatus;
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

    private final GetTicketsPageUseCase getTicketsPageUseCase;
    private final GetTicketByIdUseCase getTicketByIdUseCase;
    private final GetTicketByCodeUseCase getTicketByCodeUseCase;

    /**
     * Xem danh sách vé có phân trang (pagination)
     * Hỗ trợ sắp xếp (sort) và lọc theo tripId hoặc status (nếu có).
     *
     * Ví dụ:
     * - GET /api/tickets?page=0&size=10
     * - GET /api/tickets?page=0&size=10&sort=fare,desc
     * - GET /api/tickets?page=0&size=10&status=AVAILABLE
     * - GET /api/tickets?page=0&size=10&tripId=1
     */
    @GetMapping
    public ResponseEntity<PageResponse<TicketResponse>> getTickets(
            @PageableDefault(page = 0, size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(name = "tripId", required = false) Long tripId,
            @RequestParam(name = "status", required = false) TicketStatus status) {

        PageResponse<TicketResponse> response = getTicketsPageUseCase.execute(pageable, tripId, status);
        return ResponseEntity.ok(response);
    }

    /**
     * Tra cứu thông tin chi tiết của một vé theo ID
     *
     * Ví dụ: GET /api/tickets/1
     */
    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> getTicketById(@PathVariable("id") Long id) {
        TicketResponse response = getTicketByIdUseCase.execute(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Tra cứu thông tin chi tiết của một vé theo Mã vé (Ticket Code)
     *
     * Ví dụ: GET /api/tickets/code/TK-20261102-001
     */
    @GetMapping("/code/{ticketCode}")
    public ResponseEntity<TicketResponse> getTicketByCode(@PathVariable("ticketCode") String ticketCode) {
        TicketResponse response = getTicketByCodeUseCase.execute(ticketCode);
        return ResponseEntity.ok(response);
    }
}
