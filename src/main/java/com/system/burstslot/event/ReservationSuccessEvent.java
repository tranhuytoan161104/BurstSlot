package com.system.burstslot.event;

public record ReservationSuccessEvent(
    Long reservationId,
    Long userId,
    Long eventId
) {}
