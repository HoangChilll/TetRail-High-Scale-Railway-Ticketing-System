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
public class GetTicketByIdUseCase {

    private final TicketRepository ticketRepository;
    private final TicketAccessPolicy accessPolicy;
    private final TicketResponseCache responseCache;

    @Transactional(readOnly = true)
    public TicketResponse execute(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy vé với ID: " + id));
        accessPolicy.requireOwnerOrAdmin(ticket);
        return responseCache.get(ticket);
    }
}
