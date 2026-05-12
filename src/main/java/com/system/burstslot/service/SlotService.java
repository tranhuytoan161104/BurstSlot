package com.system.burstslot.service;

import java.util.Optional;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import com.system.burstslot.dto.SlotDto;
import com.system.burstslot.model.Slot;
import com.system.burstslot.repository.SlotRepository;

import jakarta.transaction.Transactional;

@Service
public class SlotService {
  private final SlotRepository slotRepository;

  public SlotService(SlotRepository slotRepository){
    this.slotRepository = slotRepository;
  }

  public Optional<SlotDto> getSlotsByEventId(@NonNull Long eventId){
    return slotRepository.findById(eventId)
      .map(slot -> new SlotDto(
        slot.getEventId(), 
        slot.getAvailableQuantity(),
        slot.getVersion()
      ));
  }

  
  @Transactional
  public SlotDto updateSlot(Long eventId, Integer availableQuantity){
    Slot slot = slotRepository.findById(eventId)
      .orElseThrow(() -> new RuntimeException(eventId + "not found"));;

    slot.setAvailableQuantity(availableQuantity);
    
    Slot updatedSlot = slotRepository.save(slot);

    return new SlotDto(
      updatedSlot.getEventId(),
      updatedSlot.getAvailableQuantity(),
      updatedSlot.getVersion()
    );
  }
}
