package com.system.burstslot.dto;

import java.time.OffsetDateTime;

public record CreateEventDto (
  String name,
  OffsetDateTime startTime,
  OffsetDateTime endTime,
  Integer availableQuantity
) {}
