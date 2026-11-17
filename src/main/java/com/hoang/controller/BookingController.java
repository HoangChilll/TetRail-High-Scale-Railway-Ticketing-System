package com.hoang.controller;

import com.hoang.dto.response.BookingResponse;
import com.hoang.dto.request.CreateBookingRequest;
import com.hoang.service.BookingService;
import com.hoang.security.UserDetailsCustom;
import com.hoang.security.annotation.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
            @Valid @RequestBody CreateBookingRequest command,
            @CurrentUser UserDetailsCustom currentUser) {

        Long userId = (currentUser != null && currentUser.getUser() != null) ? currentUser.getUser().getId() : null;
        BookingResponse response = bookingService.createBooking(command, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
