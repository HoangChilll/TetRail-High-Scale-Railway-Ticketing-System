package com.hoang.ddd.application.booking.usecase;

import com.hoang.ddd.application.booking.dto.response.TicketResponse;
import com.hoang.ddd.application.booking.service.TicketAccessPolicy;
import com.hoang.ddd.application.booking.service.TicketResponseCache;
import com.hoang.ddd.domain.model.entity.Ticket;
import com.hoang.ddd.domain.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetTicketByCodeUseCase {

    private final TicketRepository ticketRepository;
    private final TicketAccessPolicy accessPolicy;
    private final TicketResponseCache responseCache;

    @Transactional(readOnly = true)
    public TicketResponse execute(String ticketCode) {
        Ticket ticket = ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy vé với mã: " + ticketCode));
        accessPolicy.requireOwnerOrAdmin(ticket);
        return responseCache.get(ticket);
    }
}
