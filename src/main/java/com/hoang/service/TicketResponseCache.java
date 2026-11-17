package com.hoang.service;

import com.hoang.dto.response.TicketResponse;
import com.hoang.model.Ticket;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TicketResponseCache {
    private final CacheManager cacheManager;

    // Call only after the use case has authorized access to the current ticket.
    public TicketResponse get(Ticket ticket) {
        var cache = cacheManager.getCache("ticket_detail");
        String bookingId = ticket.getBooking() == null ? "none"
                : String.valueOf(ticket.getBooking().getId());
        String key = "authorized:v2:id:" + ticket.getId() + ":booking:" + bookingId
                + ":updated:" + ticket.getUpdatedAt();
        if (cache != null) {
            TicketResponse cached = cache.get(key, TicketResponse.class);
            if (cached != null) {
                return cached;
            }
        }
        TicketResponse response = TicketResponse.of(ticket);
        if (cache != null) {
            cache.put(key, response);
        }
        return response;
    }
}
