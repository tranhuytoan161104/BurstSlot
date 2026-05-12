package com.system.burstslot.service;

import com.system.burstslot.repository.SlotRepository;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.system.burstslot.dto.EventDto;
import com.system.burstslot.model.Event;
import com.system.burstslot.model.Slot;
import com.system.burstslot.repository.EventRepository;

import jakarta.transaction.Transactional;

@Service
public class EventService {

  private final SlotRepository slotRepository;
  private final EventRepository eventRepository;

  public EventService(EventRepository eventRepository, SlotRepository slotRepository) {
    this.eventRepository = eventRepository;
    this.slotRepository = slotRepository;
  }

  public List<EventDto> getEvents(){
    return eventRepository.findAllWithSlots();
  }

  @Transactional
  public Event createEvent(String name, OffsetDateTime startTime, OffsetDateTime endTime, Integer availableQuantity) {
    Event event = new Event();
    event.setName(name);
    event.setStartTime(startTime);
    event.setEndTime(endTime);
    event.setCreatedAt(OffsetDateTime.now());

    Event savedEvent = eventRepository.save(event);

    Slot slot = new Slot();
    slot.setEvent(savedEvent);
    slot.setAvailableQuantity(availableQuantity);

    slotRepository.save(slot);

    return savedEvent;
  } 

  @Transactional
  public void deleteEvent(Long eventId){
    slotRepository.deleteById(eventId);
    eventRepository.deleteById(eventId);
  }

}
