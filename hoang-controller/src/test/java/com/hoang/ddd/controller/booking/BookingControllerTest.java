package com.hoang.ddd.controller.booking;

import com.hoang.ddd.application.booking.usecase.BookTicketUseCase;
import com.hoang.ddd.application.booking.dto.command.CreateBookingCommand;
import com.hoang.ddd.application.booking.dto.response.BookingResponse;
import com.hoang.ddd.controller.advice.GlobalExceptionHandler;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(BookingController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class BookingControllerTest {
    @Autowired private MockMvc mvc;
    @MockBean private BookTicketUseCase useCase;

    @Test
    void rejectsMissingTicketIds() throws Exception {
        String body = """
                {"buyerName":"Learner", "buyerEmail":"learner@example.com", "buyerPhone":"0900000000"}
                """;
        mvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(useCase);
    }

    @ParameterizedTest
    @ValueSource(strings = {"[1]", "[1,2,3,4,5]"})
    void forwardsValidTicketIdsToUseCase(String ticketIds) throws Exception {
        String body = """
                {"ticketIds": %s, "buyerName":"Learner",
                 "buyerEmail":"learner@example.com", "buyerPhone":"0900000000"}
                """.formatted(ticketIds);
        when(useCase.execute(any(), isNull())).thenReturn(BookingResponse.builder().id(99L).build());
        mvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(99));
        ArgumentCaptor<CreateBookingCommand> command = ArgumentCaptor.forClass(CreateBookingCommand.class);
        verify(useCase).execute(command.capture(), isNull());
        assertThat(command.getValue().getTicketIds()).hasSize(ticketIds.equals("[1]") ? 1 : 5);
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "[]", "[null]", "[0]", "[-1]", "[1,2,3,4,5,6]"})
    void rejectsInvalidTicketIdsBeforeCallingUseCase(String ticketIds) throws Exception {
        String body = """
                {"ticketIds": %s, "buyerName": "Nguyen Van A",
                 "buyerEmail": "learner@example.com", "buyerPhone": "0900000000"}
                """.formatted(ticketIds);
        mvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Error"));
        verifyNoInteractions(useCase);
    }
}
