package com.hoang.service;

import com.hoang.dto.response.TicketResponse;
import com.hoang.dto.response.PageResponse;
import com.hoang.model.Seat;
import com.hoang.model.Ticket;
import com.hoang.model.TicketStatus;
import com.hoang.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        var responseCache = new TicketResponseCache(
                new org.springframework.cache.concurrent.ConcurrentMapCacheManager("ticket_detail"));
        ticketService = new TicketService(ticketRepository, mock(TicketAccessPolicy.class), responseCache);
    }

    private Ticket createMockTicket(Long id, String ticketCode) {
        Seat seat = Seat.builder()
                .id(10L)
                .seatNumber("A01")
                .carriageNumber("Toa 1")
                .seatClass("VIP")
                .build();

        return Ticket.builder()
                .id(id)
                .ticketCode(ticketCode)
                .seat(seat)
                .fare(BigDecimal.valueOf(500000))
                .status(TicketStatus.AVAILABLE)
                .build();
    }

    @Test
    @DisplayName("TicketService: Lấy danh sách phân trang findAll thành công")
    void getTicketsPage_FindAll_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Ticket ticket = createMockTicket(1L, "TK-001");
        Page<Ticket> page = new PageImpl<>(List.of(ticket), pageable, 1);

        when(ticketRepository.findAll(pageable)).thenReturn(page);

        PageResponse<TicketResponse> result = ticketService.getTickets(pageable, null, null);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals("TK-001", result.getContent().get(0).getTicketCode());
        assertEquals("A01", result.getContent().get(0).getSeatNumber());

        verify(ticketRepository, times(1)).findAll(pageable);
    }

    @Test
    @DisplayName("TicketService: Lấy danh sách phân trang theo tripId thành công")
    void getTicketsPage_FindByTripId_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Ticket ticket = createMockTicket(2L, "TK-002");
        Page<Ticket> page = new PageImpl<>(List.of(ticket), pageable, 1);

        when(ticketRepository.findByTripId(5L, pageable)).thenReturn(page);

        PageResponse<TicketResponse> result = ticketService.getTickets(pageable, 5L, null);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(ticketRepository, times(1)).findByTripId(5L, pageable);
    }

    @Test
    @DisplayName("TicketService: Lấy danh sách phân trang theo status thành công")
    void getTicketsPage_FindByStatus_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Ticket ticket = createMockTicket(3L, "TK-003");
        Page<Ticket> page = new PageImpl<>(List.of(ticket), pageable, 1);

        when(ticketRepository.findByStatus(TicketStatus.AVAILABLE, pageable)).thenReturn(page);

        PageResponse<TicketResponse> result = ticketService.getTickets(pageable, null, TicketStatus.AVAILABLE);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(ticketRepository, times(1)).findByStatus(TicketStatus.AVAILABLE, pageable);
    }

    @Test
    @DisplayName("TicketService: Tra vé theo ID thành công")
    void getTicketById_Success() {
        Ticket ticket = createMockTicket(1L, "TK-001");
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        TicketResponse result = ticketService.getById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("TK-001", result.getTicketCode());
        assertEquals("AVAILABLE", result.getStatus());

        verify(ticketRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("TicketService: Ném IllegalArgumentException khi không tìm thấy vé")
    void getTicketById_NotFound() {
        when(ticketRepository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ticketService.getById(999L));

        assertTrue(exception.getMessage().contains("Không tìm thấy vé với ID: 999"));
        verify(ticketRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("TicketService: Tra vé theo mã vé thành công")
    void getTicketByCode_Success() {
        Ticket ticket = createMockTicket(2L, "TK-CODE-123");
        when(ticketRepository.findByTicketCode("TK-CODE-123")).thenReturn(Optional.of(ticket));

        TicketResponse result = ticketService.getByCode("TK-CODE-123");

        assertNotNull(result);
        assertEquals(2L, result.getId());
        assertEquals("TK-CODE-123", result.getTicketCode());

        verify(ticketRepository, times(1)).findByTicketCode("TK-CODE-123");
    }

    @Test
    @DisplayName("TicketService: Ném IllegalArgumentException khi không tìm thấy vé theo mã")
    void getTicketByCode_NotFound() {
        when(ticketRepository.findByTicketCode("INVALID")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ticketService.getByCode("INVALID"));

        assertTrue(exception.getMessage().contains("Không tìm thấy vé với mã: INVALID"));
        verify(ticketRepository, times(1)).findByTicketCode("INVALID");
    }
}

