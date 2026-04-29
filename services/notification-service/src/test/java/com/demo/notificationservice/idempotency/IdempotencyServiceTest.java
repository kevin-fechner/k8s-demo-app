package com.demo.notificationservice.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

  @Mock private ProcessedEventRepository repository;

  @InjectMocks private IdempotencyService idempotencyService;

  @Test
  @DisplayName("Should return true and persist event when not yet processed")
  void tryProcess_NewEvent_ReturnsTrueAndSaves() {
    when(repository.existsByEventIdAndEventType("evt-1", "OrderCreatedEvent")).thenReturn(false);

    boolean result = idempotencyService.tryProcess("evt-1", "OrderCreatedEvent");

    assertThat(result).isTrue();
    verify(repository).save(any(ProcessedEvent.class));
  }

  @Test
  @DisplayName("Should return false and skip save when event already processed")
  void tryProcess_DuplicateEvent_ReturnsFalseWithoutSave() {
    when(repository.existsByEventIdAndEventType("evt-1", "OrderCreatedEvent")).thenReturn(true);

    boolean result = idempotencyService.tryProcess("evt-1", "OrderCreatedEvent");

    assertThat(result).isFalse();
    verify(repository, never()).save(any());
  }

  @Test
  @DisplayName("Should return false on concurrent insert (race condition)")
  void tryProcess_RaceCondition_ReturnsFalse() {
    when(repository.existsByEventIdAndEventType("evt-1", "OrderCreatedEvent")).thenReturn(false);
    when(repository.save(any(ProcessedEvent.class)))
        .thenThrow(new DataIntegrityViolationException("duplicate key"));

    boolean result = idempotencyService.tryProcess("evt-1", "OrderCreatedEvent");

    assertThat(result).isFalse();
  }
}
