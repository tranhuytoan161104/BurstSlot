package com.system.burstslot.dto;

public record ReservationRequest(
    Long eventId,
    Long userId,
    Integer quantity
) {}
