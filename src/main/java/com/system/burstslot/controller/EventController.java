package com.system.burstslot.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.system.burstslot.dto.CreateEventDto;
import com.system.burstslot.dto.EventDto;
import com.system.burstslot.dto.SlotDto;
import com.system.burstslot.model.Event;
import com.system.burstslot.service.EventService;
import com.system.burstslot.service.SlotService;

import com.system.burstslot.dto.UpdateSlotDto;



@RestController
@RequestMapping("/api/v1/events")
public class EventController {

  private final EventService eventService;
  private final SlotService slotService;

  public EventController(EventService eventService, SlotService slotService){
    this.eventService = eventService;
    this.slotService = slotService;
  }

  // Event
  @GetMapping
  public List<EventDto> getEvents() {
      return eventService.getEvents();
  }
  
  @PostMapping("/create")
  public Event createEvent(@RequestBody CreateEventDto request) {
      return eventService.createEvent(
        request.name(), request.startTime(), request.endTime(), request.availableQuantity()
      );
  }

  @DeleteMapping("/delete")
  public void deleteEvent(@PathVariable("eventId") Long eventId){
    eventService.deleteEvent(eventId);
  }

  // Slot
  @GetMapping("{eventId}/slots")
  public Optional<SlotDto> getSlot(@PathVariable("eventId") Long eventId) {
      return slotService.getSlotsByEventId(eventId);
  }

  @PatchMapping("/{eventId}/quantity")
  public SlotDto updateSlot(@PathVariable Long eventId, @RequestBody UpdateSlotDto request) {
    return slotService.updateSlot(eventId, request.availableQuantity());
  }
  
}
