package com.system.burstslot.model;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "events")
@Getter
@Setter
public class Event {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "start_time", nullable = false)
  private OffsetDateTime startTime;

  @Column(name = "end_time", nullable = false)
  private OffsetDateTime endTime;

  @Column(name = "created_at")
  private OffsetDateTime createdAt;
}
