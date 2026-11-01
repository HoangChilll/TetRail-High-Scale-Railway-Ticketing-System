package com.hoang.ddd.controller.booking;

import com.hoang.ddd.application.booking.dto.BookingResponse;
import com.hoang.ddd.application.booking.dto.CreateBookingCommand;
import com.hoang.ddd.application.booking.usecase.BookTicketUseCase;
import com.hoang.ddd.infrastructure.security.UserDetailsCustom;
import com.hoang.ddd.infrastructure.security.annotation.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookTicketUseCase bookTicketUseCase;

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
            @Valid @RequestBody CreateBookingCommand command,
            @CurrentUser UserDetailsCustom currentUser) {

        Long userId = (currentUser != null && currentUser.getUser() != null) ? currentUser.getUser().getId() : null;
        BookingResponse response = bookTicketUseCase.execute(command, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
