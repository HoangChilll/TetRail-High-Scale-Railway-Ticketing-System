package com.hoang.ddd.controller.booking;

import com.hoang.ddd.application.booking.dto.TicketResponse;
import com.hoang.ddd.application.booking.usecase.GetTicketByCodeUseCase;
import com.hoang.ddd.application.booking.usecase.GetTicketByIdUseCase;
import com.hoang.ddd.application.booking.usecase.GetTicketsPageUseCase;
import com.hoang.ddd.application.common.dto.PageResponse;
import com.hoang.ddd.controller.advice.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TicketController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetTicketsPageUseCase getTicketsPageUseCase;

    @MockBean
    private GetTicketByIdUseCase getTicketByIdUseCase;

    @MockBean
    private GetTicketByCodeUseCase getTicketByCodeUseCase;

    @Test
    @DisplayName("GET /api/tickets: Xem danh sách vé phân trang thành công 200 OK")
    void getTickets_Success() throws Exception {
        TicketResponse ticket1 = TicketResponse.builder()
                .id(1L)
                .ticketCode("TK-HN-SG-001")
                .seatNumber("A12")
                .carriageNumber("Toa 01")
                .seatClass("VIP")
                .fare(BigDecimal.valueOf(500000))
                .status("AVAILABLE")
                .build();

        PageResponse<TicketResponse> pageResponse = PageResponse.<TicketResponse>builder()
                .content(List.of(ticket1))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .empty(false)
                .build();

        when(getTicketsPageUseCase.execute(any(Pageable.class), eq(null), eq(null)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/tickets")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].ticketCode").value("TK-HN-SG-001"))
                .andExpect(jsonPath("$.content[0].seatNumber").value("A12"))
                .andExpect(jsonPath("$.content[0].status").value("AVAILABLE"));

        verify(getTicketsPageUseCase, times(1)).execute(any(Pageable.class), eq(null), eq(null));
    }

    @Test
    @DisplayName("GET /api/tickets/{id}: Tra vé theo ID thành công 200 OK")
    void getTicketById_Success() throws Exception {
        TicketResponse ticket = TicketResponse.builder()
                .id(1L)
                .ticketCode("TK-HN-SG-001")
                .passengerName("Nguyễn Văn A")
                .seatNumber("A12")
                .carriageNumber("Toa 01")
                .seatClass("VIP")
                .fare(BigDecimal.valueOf(500000))
                .status("ISSUED")
                .build();

        when(getTicketByIdUseCase.execute(1L)).thenReturn(ticket);

        mockMvc.perform(get("/api/tickets/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.ticketCode").value("TK-HN-SG-001"))
                .andExpect(jsonPath("$.passengerName").value("Nguyễn Văn A"))
                .andExpect(jsonPath("$.seatNumber").value("A12"))
                .andExpect(jsonPath("$.status").value("ISSUED"));

        verify(getTicketByIdUseCase, times(1)).execute(1L);
    }

    @Test
    @DisplayName("GET /api/tickets/{id}: Tra vé theo ID không tồn tại trả về 400 Bad Request")
    void getTicketById_NotFound() throws Exception {
        when(getTicketByIdUseCase.execute(999L))
                .thenThrow(new IllegalArgumentException("Không tìm thấy vé với ID: 999"));

        mockMvc.perform(get("/api/tickets/999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Không tìm thấy vé với ID: 999"));

        verify(getTicketByIdUseCase, times(1)).execute(999L);
    }

    @Test
    @DisplayName("GET /api/tickets/code/{ticketCode}: Tra vé theo Mã vé thành công 200 OK")
    void getTicketByCode_Success() throws Exception {
        TicketResponse ticket = TicketResponse.builder()
                .id(2L)
                .ticketCode("TK-TEST-999")
                .seatNumber("B05")
                .carriageNumber("Toa 02")
                .seatClass("STANDARD")
                .fare(BigDecimal.valueOf(300000))
                .status("AVAILABLE")
                .build();

        when(getTicketByCodeUseCase.execute("TK-TEST-999")).thenReturn(ticket);

        mockMvc.perform(get("/api/tickets/code/TK-TEST-999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.ticketCode").value("TK-TEST-999"))
                .andExpect(jsonPath("$.seatNumber").value("B05"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));

        verify(getTicketByCodeUseCase, times(1)).execute("TK-TEST-999");
    }

    @Test
    @DisplayName("GET /api/tickets/code/{ticketCode}: Tra vé theo Mã không tồn tại trả về 400 Bad Request")
    void getTicketByCode_NotFound() throws Exception {
        when(getTicketByCodeUseCase.execute("INVALID-CODE"))
                .thenThrow(new IllegalArgumentException("Không tìm thấy vé với mã: INVALID-CODE"));

        mockMvc.perform(get("/api/tickets/code/INVALID-CODE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Không tìm thấy vé với mã: INVALID-CODE"));

        verify(getTicketByCodeUseCase, times(1)).execute("INVALID-CODE");
    }
}

