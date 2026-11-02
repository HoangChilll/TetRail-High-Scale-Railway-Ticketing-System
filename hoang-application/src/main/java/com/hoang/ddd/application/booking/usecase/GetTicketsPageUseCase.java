package com.hoang.ddd.application.booking.usecase;

import com.hoang.ddd.application.booking.dto.TicketResponse;
import com.hoang.ddd.application.common.dto.PageResponse;
import com.hoang.ddd.domain.model.entity.Ticket;
import com.hoang.ddd.domain.model.entity.TicketStatus;
import com.hoang.ddd.domain.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetTicketsPageUseCase {

    private final TicketRepository ticketRepository;

    @Transactional(readOnly = true)
    public PageResponse<TicketResponse> execute(Pageable pageable, Long tripId, TicketStatus status) {
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
}

