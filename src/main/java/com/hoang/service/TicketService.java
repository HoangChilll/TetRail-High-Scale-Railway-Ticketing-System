package com.hoang.service;

import com.hoang.dto.response.PageResponse;
import com.hoang.dto.response.SeatAvailabilityResponse;
import com.hoang.dto.response.TicketResponse;
import com.hoang.model.Ticket;
import com.hoang.model.TicketStatus;
import com.hoang.repository.TicketRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;

    private final TicketAccessPolicy accessPolicy;

    private final TicketResponseCache responseCache;

    @Transactional(readOnly = true)
    public TicketResponse getById(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy vé với ID: " + id));
        accessPolicy.requireOwnerOrAdmin(ticket);
        return responseCache.get(ticket);
    }

    @Transactional(readOnly = true)
    public TicketResponse getByCode(String ticketCode) {
        Ticket ticket = ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy vé với mã: " + ticketCode));
        accessPolicy.requireOwnerOrAdmin(ticket);
        return responseCache.get(ticket);
    }

    @Transactional(readOnly = true)
    public PageResponse<TicketResponse> getTickets(Pageable pageable, Long tripId, TicketStatus status) {
        accessPolicy.requireAdmin();
        Page<Ticket> ticketPage;
        if (tripId != null) {
            ticketPage = ticketRepository.findByTripId(tripId, pageable);
        } else if (status != null) {
            ticketPage = ticketRepository.findByStatus(status, pageable);
        } else {
            ticketPage = ticketRepository.findAll(pageable);
        }
        return PageResponse.of(ticketPage.map(TicketResponse::of));
    }

    @Cacheable(value = "trip_tickets", key = "'seats:v2:' + #tripId", unless = "#result == null || #result.isEmpty()")
    @Transactional(readOnly = true)
    public List<SeatAvailabilityResponse> getTripTickets(Long tripId) {
        return ticketRepository.findByTripId(tripId)
                .stream()
                .map(SeatAvailabilityResponse::of)
                .toList();
    }
}
