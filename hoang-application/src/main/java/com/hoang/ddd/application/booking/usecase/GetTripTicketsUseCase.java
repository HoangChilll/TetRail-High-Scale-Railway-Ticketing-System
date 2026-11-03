package com.hoang.ddd.application.booking.usecase;

import com.hoang.ddd.application.booking.dto.TicketResponse;
import com.hoang.ddd.domain.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GetTripTicketsUseCase {

    private final TicketRepository ticketRepository;

    @Cacheable(value = "trip_tickets", key = "#tripId", unless = "#result == null || #result.isEmpty()")
    @Transactional(readOnly = true)
    public List<TicketResponse> execute(Long tripId) {
        return ticketRepository.findByTripId(tripId)
                .stream()
                .map(TicketResponse::of)
                .toList();
    }
}
