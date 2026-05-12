package com.system.burstslot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.system.burstslot.model.Slot;

public interface SlotRepository extends JpaRepository<Slot, Long> {
}