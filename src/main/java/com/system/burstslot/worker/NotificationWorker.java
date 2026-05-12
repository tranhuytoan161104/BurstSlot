package com.system.burstslot.worker;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationWorker {

    @KafkaListener(
    topics = "reservation.success", 
    groupId = "burstslot-worker-group"
    )
    public void handleReservationSuccessEvent(String messageJson) {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}