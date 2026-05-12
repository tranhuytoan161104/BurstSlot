package com.system.burstslot.worker;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationWorker {

    @KafkaListener(
    topics = "booking.success", 
    groupId = "burstslot-worker-group"
    )
    public void handleBookingSuccessEvent(String messageJson) {
        try {
            System.out.println("\n[KAFKA WORKER] Đã nhận thông điệp: " + messageJson);
            
            System.out.println("[KAFKA WORKER] Bắt đầu kết nối tới máy chủ SMTP để gửi Email...");
            Thread.sleep(3000); 
            
            System.out.println("[KAFKA WORKER] Gửi Email xác nhận thành công!\n");
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Lỗi trong quá trình xử lý hậu kỳ: " + e.getMessage());
        }
    }
}