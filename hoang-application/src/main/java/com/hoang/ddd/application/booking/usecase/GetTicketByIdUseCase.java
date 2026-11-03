package com.hoang.ddd.application.booking.usecase;

import com.hoang.ddd.application.booking.dto.TicketResponse;
import com.hoang.ddd.domain.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;

@Service
@RequiredArgsConstructor
public class GetTicketByIdUseCase {

    private final TicketRepository ticketRepository;

    @Cacheable(value = "ticket_detail", key = "'id:' + #id")
    @Transactional(readOnly = true)
    public TicketResponse execute(Long id) {
        return ticketRepository.findById(id)
                .map(TicketResponse::of)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy vé với ID: " + id));
    }
}
