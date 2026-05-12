package com.system.burstslot.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.system.burstslot.repository.SlotRepository;
import com.system.burstslot.repository.ReservationRepository;
import com.system.burstslot.dto.BookingRequest;
import com.system.burstslot.model.Reservation;
import com.system.burstslot.event.BookingSuccessEvent;
import com.system.burstslot.repository.OutboxEventRepository;
import com.system.burstslot.model.OutboxEvent;

@Service
public class ReservationService {

    private final SlotRepository slotRepository;
    private final ReservationRepository reservationRepository;
    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final OutboxEventRepository outboxEventRepository;

    public ReservationService(SlotRepository slotRepository, ReservationRepository reservationRepository, 
                              StringRedisTemplate redisTemplate, KafkaTemplate<String, String> kafkaTemplate, 
                              ObjectMapper objectMapper, OutboxEventRepository outboxEventRepository) {
        this.slotRepository = slotRepository;
        this.reservationRepository = reservationRepository;
        this.redisTemplate = redisTemplate;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.outboxEventRepository = outboxEventRepository;
    }

    @Transactional
    public void createBooking(BookingRequest request) {
        String redisKey = "event:" + request.eventId() + ":tickets";

        Long remainingTickets = redisTemplate.opsForValue().decrement(redisKey, request.quantity());

        if (remainingTickets == null || remainingTickets < 0) {
            redisTemplate.opsForValue().increment(redisKey, request.quantity());
            throw new RuntimeException("Rất tiếc, vé đã bán hết!"); 
        }

        try {
            int updatedRows = slotRepository.decrementSlot(request.eventId(), request.quantity());

            if (updatedRows == 0) {
                throw new RuntimeException("Lệch pha dữ liệu: CSDL không đủ vé để cấp phát.");
            }

            Reservation res = new Reservation();
            res.setEventId(request.eventId());
            res.setUserId(request.userId());
            res.setQuantity(request.quantity());
            res.setStatus("SUCCESS");
            Reservation savedRes = reservationRepository.save(res);

            BookingSuccessEvent eventData = new BookingSuccessEvent(savedRes.getId(), savedRes.getUserId(), savedRes.getEventId());
            String payloadJson = objectMapper.writeValueAsString(eventData);
            
            OutboxEvent outboxEvent = new OutboxEvent();
            outboxEvent.setEventType("BOOKING_SUCCESS");
            outboxEvent.setPayload(payloadJson);
            outboxEvent.setAggregateId(String.valueOf(savedRes.getUserId()));
            outboxEvent.setStatus("PENDING"); 
            
            outboxEventRepository.save(outboxEvent);

        } catch (Exception ex) {
            redisTemplate.opsForValue().increment(redisKey, request.quantity());
            throw new RuntimeException("Giao dịch thất bại, vé đã được hoàn lại. Lỗi: " + ex.getMessage());
        }
    }
}