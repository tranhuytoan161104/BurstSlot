package com.system.burstslot.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.system.burstslot.dto.ReservationRequest;
import com.system.burstslot.service.ReservationService;

@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public ResponseEntity<String> createReservation(
            @RequestBody ReservationRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        
        reservationService.createReservation(request, idempotencyKey);
        return ResponseEntity.ok("Thành công");
    }
}