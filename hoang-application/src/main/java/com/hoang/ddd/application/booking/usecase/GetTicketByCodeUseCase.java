package com.hoang.ddd.application.booking.usecase;

import com.hoang.ddd.application.booking.dto.TicketResponse;
import com.hoang.ddd.domain.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetTicketByCodeUseCase {

    private final TicketRepository ticketRepository;

    @Transactional(readOnly = true)
    public TicketResponse execute(String ticketCode) {
        return ticketRepository.findByTicketCode(ticketCode)
                .map(TicketResponse::of)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy vé với mã: " + ticketCode));
    }
}

