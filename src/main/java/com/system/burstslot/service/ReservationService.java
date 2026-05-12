package com.system.burstslot.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.system.burstslot.repository.SlotRepository;
import com.system.burstslot.repository.ReservationRepository;
import com.system.burstslot.dto.BookingRequest;
import com.system.burstslot.model.Reservation;
import com.system.burstslot.model.Slot;

@Service
public class ReservationService {

    private final SlotRepository slotRepository;
    private final ReservationRepository reservationRepository;

    public ReservationService(SlotRepository slotRepository, ReservationRepository reservationRepository) {
        this.slotRepository = slotRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional
    public void createBooking(BookingRequest request) {
        Slot slot = slotRepository.findById(request.eventId())
            .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện"));

        if (slot.getAvailableQuantity() < request.quantity()) {
            throw new RuntimeException("Rất tiếc, vé đã bán hết!");
        }

        slot.setAvailableQuantity(slot.getAvailableQuantity() - request.quantity());
        slotRepository.save(slot);

        Reservation res = new Reservation();
        res.setEventId(request.eventId());
        res.setUserId(request.userId());
        res.setQuantity(request.quantity());
        res.setStatus("SUCCESS");
        reservationRepository.save(res);
    }
}