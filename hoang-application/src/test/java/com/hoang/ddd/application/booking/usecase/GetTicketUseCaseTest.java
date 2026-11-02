package com.hoang.ddd.application.booking.usecase;

import com.hoang.ddd.application.booking.dto.TicketResponse;
import com.hoang.ddd.application.common.dto.PageResponse;
import com.hoang.ddd.domain.model.entity.Seat;
import com.hoang.ddd.domain.model.entity.Ticket;
import com.hoang.ddd.domain.model.entity.TicketStatus;
import com.hoang.ddd.domain.repository.TicketRepository;
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
class GetTicketUseCaseTest {

    @Mock
    private TicketRepository ticketRepository;

    private GetTicketsPageUseCase getTicketsPageUseCase;
    private GetTicketByIdUseCase getTicketByIdUseCase;
    private GetTicketByCodeUseCase getTicketByCodeUseCase;

    @BeforeEach
    void setUp() {
        getTicketsPageUseCase = new GetTicketsPageUseCase(ticketRepository);
        getTicketByIdUseCase = new GetTicketByIdUseCase(ticketRepository);
        getTicketByCodeUseCase = new GetTicketByCodeUseCase(ticketRepository);
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
    @DisplayName("GetTicketsPageUseCase: Lấy danh sách phân trang findAll thành công")
    void getTicketsPage_FindAll_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Ticket ticket = createMockTicket(1L, "TK-001");
        Page<Ticket> page = new PageImpl<>(List.of(ticket), pageable, 1);

        when(ticketRepository.findAll(pageable)).thenReturn(page);

        PageResponse<TicketResponse> result = getTicketsPageUseCase.execute(pageable, null, null);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals("TK-001", result.getContent().get(0).getTicketCode());
        assertEquals("A01", result.getContent().get(0).getSeatNumber());

        verify(ticketRepository, times(1)).findAll(pageable);
    }

    @Test
    @DisplayName("GetTicketsPageUseCase: Lấy danh sách phân trang theo tripId thành công")
    void getTicketsPage_FindByTripId_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Ticket ticket = createMockTicket(2L, "TK-002");
        Page<Ticket> page = new PageImpl<>(List.of(ticket), pageable, 1);

        when(ticketRepository.findByTripId(5L, pageable)).thenReturn(page);

        PageResponse<TicketResponse> result = getTicketsPageUseCase.execute(pageable, 5L, null);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(ticketRepository, times(1)).findByTripId(5L, pageable);
    }

    @Test
    @DisplayName("GetTicketsPageUseCase: Lấy danh sách phân trang theo status thành công")
    void getTicketsPage_FindByStatus_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Ticket ticket = createMockTicket(3L, "TK-003");
        Page<Ticket> page = new PageImpl<>(List.of(ticket), pageable, 1);

        when(ticketRepository.findByStatus(TicketStatus.AVAILABLE, pageable)).thenReturn(page);

        PageResponse<TicketResponse> result = getTicketsPageUseCase.execute(pageable, null, TicketStatus.AVAILABLE);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(ticketRepository, times(1)).findByStatus(TicketStatus.AVAILABLE, pageable);
    }

    @Test
    @DisplayName("GetTicketByIdUseCase: Tra vé theo ID thành công")
    void getTicketById_Success() {
        Ticket ticket = createMockTicket(1L, "TK-001");
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        TicketResponse result = getTicketByIdUseCase.execute(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("TK-001", result.getTicketCode());
        assertEquals("AVAILABLE", result.getStatus());

        verify(ticketRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("GetTicketByIdUseCase: Ném IllegalArgumentException khi không tìm thấy vé")
    void getTicketById_NotFound() {
        when(ticketRepository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> getTicketByIdUseCase.execute(999L));

        assertTrue(exception.getMessage().contains("Không tìm thấy vé với ID: 999"));
        verify(ticketRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("GetTicketByCodeUseCase: Tra vé theo mã vé thành công")
    void getTicketByCode_Success() {
        Ticket ticket = createMockTicket(2L, "TK-CODE-123");
        when(ticketRepository.findByTicketCode("TK-CODE-123")).thenReturn(Optional.of(ticket));

        TicketResponse result = getTicketByCodeUseCase.execute("TK-CODE-123");

        assertNotNull(result);
        assertEquals(2L, result.getId());
        assertEquals("TK-CODE-123", result.getTicketCode());

        verify(ticketRepository, times(1)).findByTicketCode("TK-CODE-123");
    }

    @Test
    @DisplayName("GetTicketByCodeUseCase: Ném IllegalArgumentException khi không tìm thấy vé theo mã")
    void getTicketByCode_NotFound() {
        when(ticketRepository.findByTicketCode("INVALID")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> getTicketByCodeUseCase.execute("INVALID"));

        assertTrue(exception.getMessage().contains("Không tìm thấy vé với mã: INVALID"));
        verify(ticketRepository, times(1)).findByTicketCode("INVALID");
    }
}

