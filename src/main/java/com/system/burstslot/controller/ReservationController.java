package com.system.burstslot.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.system.burstslot.dto.BookingRequest;
import com.system.burstslot.service.ReservationService;

@RestController
@RequestMapping("/api/v1/bookings")
public class ReservationController {

    private final ReservationService bookingService;

    public ReservationController(ReservationService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<String> bookSlot(@RequestBody BookingRequest request) {
        bookingService.createBooking(request);
        return ResponseEntity.ok("Thành công");
    }
}