package com.system.burstslot.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import com.system.burstslot.repository.SlotRepository;
import com.system.burstslot.model.Slot;

@Component
public class CacheWarmer implements CommandLineRunner {

    private final StringRedisTemplate redisTemplate;
    private final SlotRepository slotRepository;

    public CacheWarmer(StringRedisTemplate redisTemplate, SlotRepository slotRepository) {
        this.redisTemplate = redisTemplate;
        this.slotRepository = slotRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        Slot slot = slotRepository.findById(1L).orElse(null);
        if (slot != null) {
            String redisKey = "event:1:tickets";
            redisTemplate.opsForValue().set(redisKey, String.valueOf(slot.getAvailableQuantity()));
        }
    }
}