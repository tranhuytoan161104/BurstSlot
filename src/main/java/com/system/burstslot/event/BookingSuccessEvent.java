package com.system.burstslot.event;

public record BookingSuccessEvent(
    Long reservationId,
    Long userId,
    Long eventId
) {}