package com.demo.productservice.idempotency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final ProcessedEventRepository repository;

    /**
     * Returns true if this event should be processed (not yet seen).
     * Returns false if it was already processed (duplicate - skip it).
     */
    @Transactional
    public boolean tryProcess(String eventId, String eventType) {
        if (repository.existsByEventIdAndEventType(eventId, eventType)) {
            log.info("Skipping duplicate event id={} type={}", eventId, eventType);
            return false;
        }
        try {
            repository.save(new ProcessedEvent(eventId, eventType));
            return true;
        } catch (DataIntegrityViolationException e) {
            // Race condition: another pod inserted between our check and save
            log.info("Race condition detected for event id={} type={} - skipping", eventId, eventType);
            return false;
        }
    }
}