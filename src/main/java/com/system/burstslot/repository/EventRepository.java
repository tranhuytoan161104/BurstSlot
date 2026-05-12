package com.system.burstslot.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.system.burstslot.dto.EventDto;
import com.system.burstslot.model.Event;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
  @Query("SELECT new com.system.burstslot.dto.EventDto(" +
           "e.id, e.name, e.startTime, e.endTime, " +
           "COALESCE(s.availableQuantity, 0)) " +
           "FROM Event e LEFT JOIN Slot s ON e.id = s.event.id")
  List<EventDto> findAllWithSlots();
}
