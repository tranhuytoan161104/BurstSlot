package com.system.burstslot.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.system.burstslot.repository.OutboxEventRepository;
import com.system.burstslot.model.OutboxEvent;
import java.util.List;

@Service
public class OutboxRelayService {

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxRelayService(OutboxEventRepository outboxRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 5000)
    public void processOutboxEvents() {
        List<OutboxEvent> pendingEvents = outboxRepository.findByStatus("PENDING");

        if (pendingEvents.isEmpty()) return;


        for (OutboxEvent event : pendingEvents) {
            try {
                kafkaTemplate.send("booking.success", event.getAggregateId(), event.getPayload()).get();
                
                event.setStatus("SENT");
                outboxRepository.save(event);

            } catch (Exception e) {
            }
        }
    }
}