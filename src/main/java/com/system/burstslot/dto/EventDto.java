package com.system.burstslot.dto;

import java.time.OffsetDateTime;

public record EventDto (
    Long id, 
    String name, 
    OffsetDateTime startTime, 
    OffsetDateTime endTime,
    Integer availableQuantity,
    Long slotVersion
){}
