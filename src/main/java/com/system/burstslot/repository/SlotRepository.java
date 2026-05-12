package com.system.burstslot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.system.burstslot.model.Slot;

public interface SlotRepository extends JpaRepository<Slot, Long> {
    
    @Modifying
    @Query("UPDATE Slot s SET s.availableQuantity = s.availableQuantity - :quantity WHERE s.eventId = :eventId AND s.availableQuantity >= :quantity")
    int decrementSlot(@Param("eventId") Long eventId, @Param("quantity") Integer quantity);
}