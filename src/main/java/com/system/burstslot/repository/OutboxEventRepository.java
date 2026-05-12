package com.system.burstslot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.system.burstslot.model.OutboxEvent;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    
    List<OutboxEvent> findByStatus(String status);
    
}