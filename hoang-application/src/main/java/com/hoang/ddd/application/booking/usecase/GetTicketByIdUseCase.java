package com.hoang.ddd.application.booking.usecase;

import com.hoang.ddd.application.booking.dto.TicketResponse;
import com.hoang.ddd.domain.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetTicketByIdUseCase {

    private final TicketRepository ticketRepository;

    @Transactional(readOnly = true)
    public TicketResponse execute(Long id) {
        return ticketRepository.findById(id)
                .map(TicketResponse::of)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy vé với ID: " + id));
    }
}

