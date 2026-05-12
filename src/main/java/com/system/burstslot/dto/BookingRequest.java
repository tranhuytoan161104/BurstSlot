package com.system.burstslot.dto;

public record BookingRequest(
    Long eventId,
    Long userId,
    Integer quantity
) {}